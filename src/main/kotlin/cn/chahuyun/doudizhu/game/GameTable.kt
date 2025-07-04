package cn.chahuyun.doudizhu.game

import cn.chahuyun.doudizhu.*
import cn.chahuyun.doudizhu.Cards.Companion.show
import cn.chahuyun.doudizhu.util.MessageUtil.nextMessage
import cn.chahuyun.doudizhu.util.PlayerUtil
import kotlinx.coroutines.withTimeoutOrNull
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.MemberPermission
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.PlainText

/**
 * 一个游戏桌上的对局状态
 */
enum class GameStatus {
    /**
     * 刚开始
     */
    START,

    /**
     * 抢地主
     */
    DIZHU,

    /**
     * 战斗中
     */
    BATTLE,

    /**
     * 结束
     */
    STOP
}

interface GameTableProcess {

    /**
     * ->游戏开始
     * ->检查好友，开启禁言，发牌
     * ->进入轮询消息监听，开始对局
     */
    suspend fun start()

    /**
     * ->抢地主阶段
     * ->决定地主，补牌
     */
    suspend fun dizhu()

    /**
     * 出牌?
     */
    suspend fun cards()

    /**
     * ->对局结束
     * ->计算倍率，计算积分，操作结果
     */
    suspend fun stop()
}


/**
 * 游戏桌
 */
@Suppress("SpellCheckingInspection")
class GameTable(
    /**
     * 玩家
     */
    val players: List<Player>,
    /**
     * bot
     */
    private val bot: Bot,
    /**
     * 群
     */
    private val group: Group,
    /**
     * 游戏状态
     */
    private var status: GameStatus = GameStatus.START,

    /**
     * 牌库
     */
    private val deck: List<Car> = Cards.createFullExpandDeck(),

    ) : GameTableProcess {

    companion object {
        //回复超时时间
        private const val CONFIG_TIMEOUT_SECONDS = 30L

        //最低底分
        private const val MIN_BET = 1

        //最高底分
        private const val MAX_BET = 1000
    }

    /**
     * 对局信息
     */
    private lateinit var game: Game

    /**
     * 底牌，先用空代替
     */
    private lateinit var bottomCards: List<Car>

    /**
     * ->游戏开始
     * ->检查好友，开启禁言，发牌
     * ->进入轮询消息监听，开始对局
     */
    override suspend fun start() {
        if (group.botPermission != MemberPermission.OWNER) {
            sendMessage("本${DZConfig.botName}不是群主哦~")
            cancelGame()
            return
        }

        sendMessage("游戏开始,请在1分钟内添加本bot的好友!")

        withTimeoutOrNull(60 * 1000L) {
            val util = PlayerUtil(players)
            if (util.check()) return@withTimeoutOrNull
            if (util.listening()) return@withTimeoutOrNull
        } ?: run {
            players.forEach {
                if (bot.getFriend(it.id) == null) {
                    sendMessage("${it.name} 还不是本${DZConfig.botName}的好友哦!")
                    cancelGame()
                    return
                }
            }
        }

        val player = players[0]
        sendMessage(player.id, "请配置底分($MIN_BET~$MAX_BET),发送掀桌停止!")

        var bottom = 0
        while (bottom == 0) {
            val nextMessage = nextMessage(player, CONFIG_TIMEOUT_SECONDS.toInt()) ?: run {
                sendMessage("配置超时,(╯‵□′)╯︵┻━┻")
                cancelGame()
                return
            }

            val input = nextMessage.message.contentToString().trim()

            if (input == "掀桌") {
                sendMessage("${player.name} 停止了对局!")
                cancelGame()
                return
            }

            val bet = input.toIntOrNull()
            if (bet != null && bet in MIN_BET..MAX_BET) {
                sendMessage("${player.name} 配置底分为: $bet!")
                bottom = bet
            } else {
                sendMessage("请输入有效的底分（$MIN_BET~${MAX_BET}）")
            }
        }

        game = Game(players, group.id, bottom, 0)

        // 分配底牌
        bottomCards = deck.take(3)
        // 剩余的牌分配给玩家
        val playerCards = deck.drop(3)

        // 发牌逻辑 - 每人17张牌
        var nextPlayerIndex = 0
        for (card in playerCards) {
            game.addHand(nextPlayerIndex, card)
            nextPlayerIndex = (nextPlayerIndex + 1) % game.players.size
        }

        game.players.forEach {
            group[it.id]?.modifyAdmin(true) ?: run {
                sendMessage("有一位牌友不在群里，游戏失败")
                try {
                    game.players.forEach { at -> group[at.id]?.modifyAdmin(false) }
                } catch (_: Exception) {
                }
                return
            }
        }

        group.settings.isMuteAll = true

        game.players.forEach { it.sendMessage("你的手牌:\n ${it.toHand()}") }

        sendMessage("游戏开始，请移至好友查看手牌!")
    }


    /**
     * ->抢地主阶段
     * ->决定地主，补牌
     */
    override suspend fun dizhu() {
        status = GameStatus.DIZHU
        var nextPlayer: Player

        var landlord: Player? = null
        // 记录每位玩家是否曾经抢过地主
        val qiang = players.associateWith { false }.toMutableMap()
        var round = 1
        var timer = 0
        while (true) {
            nextPlayer = players[timer % 3]
            if (round == 1) {
                //第一轮
                sendMessage(nextPlayer.id, "开始抢地主:(抢/抢地主)")
                val nextMessage = nextMessage(nextPlayer) ?: run {
                    sendMessage("发送超时,(╯‵□′)╯︵┻━┻")
                    cancelGame()
                    return
                }

                val content = nextMessage.message.contentToString()
                if (content.matches(Regex("^抢|抢地主"))) {
                    landlord = nextPlayer
                    qiang[nextPlayer] = true
                    sendMessage("${nextPlayer.name} 抢地主！")
                }
                timer++
                if (timer == 3) {
                    //三人操作完成,验证后进行第二轮
                    round = 2
                    val stillBidding = qiang.filterValues { it }.keys.toList()
                    if (stillBidding.size == 1) {
                        break
                    } else if (stillBidding.isEmpty()) {
                        // TODO: 可以后期再决定是否重新发牌/重置游戏
                        landlord = players.random()
                        sendMessage("无人角逐地主,那本${DZConfig.botName}就随便指定一个了!恭喜${landlord.name}成功地主!")
                        break
                    } else {
                        continue
                    }
                }
            }

            //第二轮
            if (round == 2) {
                if (qiang[nextPlayer]!!) {
                    sendMessage(nextPlayer.id, "开始角逐抢地主:(抢/抢地主)")
                    val nextMessage = nextMessage(nextPlayer) ?: run {
                        sendMessage("发送超时,(╯‵□′)╯︵┻━┻")
                        cancelGame()
                        return
                    }

                    val content = nextMessage.message.contentToString()

                    if (content.matches(Regex("^抢|抢地主"))) {
                        landlord = nextPlayer
                        break
                    } else {
                        val stillBidding = qiang.filterValues { it }.keys.toList()
                        if (stillBidding.size == 2) {
                            //如果是两个人的角逐,排除当前人,直接让另外一个人上位
                            landlord = stillBidding.first { it != nextPlayer }
                            break
                        } else {
                            //如果不是两个人,则这个人退出角逐,下一个人的角逐必定是2个人,因为一个人在第一轮就已经确定了
                            qiang[nextPlayer] = false
                        }
                    }
                }
                timer++
            }
        }

        if (landlord == null) {
            sendMessage("系统错误!")
            return
        }

        bottomCards.forEach { landlord.addHand(it) }
        sendMessage(
            """
            底牌是:${bottomCards.show()}
        """.trimIndent()
        )

        landlord.sendMessage("你的手牌:\n" + " ${landlord.toHand()}")
        sendMessage("${landlord.name} 抢到了地主!请开始出牌!")

        cards()
    }

    /**
     * 出牌?
     */
    override suspend fun cards() {
        status = GameStatus.BATTLE
        //进入战斗!
        TODO("Not yet implemented")
    }

    /**
     * ->对局结束
     * ->计算倍率，计算积分，操作结果
     */
    override suspend fun stop() {
        TODO("Not yet implemented")
    }

    private suspend fun GameTable.sendMessage(msg: String) {
        this.group.sendMessage(msg)
    }

    private suspend fun GameTable.sendMessage(id: Long, msg: String) {
        this.group.sendMessage(At(id).plus(PlainText(msg)))
    }

    private suspend fun GameTable.sendMessage(player: Player, msg: String) {
        this.group.sendMessage(At(player.id).plus(PlainText(msg)))
    }

    private suspend fun Player.sendMessage(msg: String) {
        bot.getFriend(this.id)?.sendMessage(msg)
    }

    private suspend fun GameTable.cancelGame() {
        GameEvent.cancelGame(group)
    }

    /**
     * 获取好友的下一条消息
     */
    suspend fun Player.nextMessage(): MessageEvent? = nextMessage(this, 30)
}

