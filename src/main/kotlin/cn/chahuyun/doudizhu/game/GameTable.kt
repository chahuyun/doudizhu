package cn.chahuyun.doudizhu.game

import cn.chahuyun.doudizhu.*
import cn.chahuyun.doudizhu.util.MessageUtil.nextMessage
import cn.chahuyun.doudizhu.util.PlayerUtil
import kotlinx.coroutines.withTimeoutOrNull
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.MemberPermission
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

        game.players.forEach {
            bot.getFriend(it.id)?.sendMessage("你的手牌:\n ${it.toHand()}")
        }

        sendMessage("游戏开始，请移至好友查看手牌!")
    }


    /**
     * ->抢地主阶段
     * ->决定地主，补牌
     */
    override suspend fun dizhu() {
        status = GameStatus.DIZHU
        val nextPlayer = game.nextPlayer

        var landlord: Player
        while (true) {
            sendMessage(nextPlayer.id, "开始抢地主:(抢/抢地主)")
            val nextMessage = nextMessage(nextPlayer)


        }


        TODO("Not yet implemented")
    }

    /**
     * 出牌?
     */
    override suspend fun cards() {
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

    private suspend fun GameTable.cancelGame() {
        GameEvent.cancelGame(group)
    }
}

