package cn.chahuyun.doudizhu.game

import cn.chahuyun.doudizhu.*
import cn.chahuyun.doudizhu.Car.Companion.toListCards
import cn.chahuyun.doudizhu.Cards.Companion.cardsShow
import cn.chahuyun.doudizhu.Cards.Companion.show
import cn.chahuyun.doudizhu.Cards.Companion.toListCar
import cn.chahuyun.doudizhu.game.CardFormUtil.check
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

        sendMessage("游戏开始,请在2分钟内添加本bot的好友!")

        withTimeoutOrNull(120 * 1000L) {
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

        game = Game(players, group.id, bottom, 1)

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

        dizhu()
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
                } else {
                    sendMessage("${nextPlayer.name} 不抢。")
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
                        game.fold *= 2
                        break
                    } else {
                        val stillBidding = qiang.filterValues { it }.keys.toList()
                        if (stillBidding.size == 2) {
                            //如果是两个人的角逐,排除当前人,直接让另外一个人上位
                            landlord = stillBidding.first { it != nextPlayer }
                            break
                        } else {
                            sendMessage("${nextPlayer.name} 不抢。")
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

        if (bottomCards.toListCards().size == 1) {
            sendMessage("底牌是3连:${bottomCards.show()},翻倍!")
            game.fold *= 2
        }else if (bottomCards.toListCards().contains(Car.BIG_JOKER) && bottomCards.toListCards().contains(Car.SMALL_JOKER)){
            sendMessage("底牌是王炸:${bottomCards.show()},翻倍!")
            game.fold *= 2
        }else{
            sendMessage("底牌是:${bottomCards.show()}")
        }

        game.landlord = landlord
        game.setNextPlayer(landlord)
        landlord.sendMessage("你的手牌:\n" + " ${landlord.toHand()}")
        sendMessage("${landlord.name} 抢到了地主!请开始出牌!")

        cards()
    }

    /**
     * 出牌?
     */
    @Suppress("DuplicatedCode")
    override suspend fun cards() {
        status = GameStatus.BATTLE
        var player = game.nextPlayer

        // 比较的玩家
        var maxPlayer: Player? = null
        // 比较的手牌
        var maxCards: List<Cards> = mutableListOf()
        // 比较的牌类型
        var maxForm: CardForm = CardForm.ERROR
        var win: Player

        // 提取出牌后的公共操作
        suspend fun handlePlay(player: Player, cards: List<Cards>, match: CardForm, isFirst: Boolean) {
            val action = if (isFirst) "出牌" else "管上"
            val msg = when (match) {
                CardForm.BOMB -> "${player.name} : 炸弹(翻倍)! ${cards.cardsShow()}"
                CardForm.TRIPLE_ONE -> "${player.name} : 三带一! ${cards.cardsShow()}"
                CardForm.TRIPLE_TWO -> "${player.name} : 三带二! ${cards.cardsShow()}"
                CardForm.QUEUE -> "${player.name} : 顺子! ${cards.cardsShow()}"
                CardForm.GHOST_BOMB -> "${player.name} : 王炸(翻倍)!!!"
                CardForm.QUEUE_TWO -> "${player.name} : 连对! ${cards.cardsShow()}"
                CardForm.AIRCRAFT -> "${player.name} : 飞机! ${cards.cardsShow()}"
                CardForm.AIRCRAFT_SINGLE -> "${player.name} : 飞机! ${cards.cardsShow()}"
                CardForm.AIRCRAFT_PAIR -> "${player.name} : 飞机! ${cards.cardsShow()}"
                else -> "${player.name} : $action ${cards.cardsShow()}"
            }

            if (player.playCards(cards)) {
                sendMessage(msg)
                val handSize = player.hand.sumOf { it.num }
                if (0 < handSize && handSize  <= 3) {
                    sendMessage("${player.name} 只剩 $handSize 张牌了!")
                }

                maxPlayer = player
                maxForm = match
                maxCards = cards
            } else {
                sendMessage("错误!")
            }
        }

        while (true) {
            val isFirst = maxPlayer == null || maxPlayer == player
            sendMessage(player, "请出牌!" + if (isFirst) "" else "或者过")

            val nextMessage = player.nextMessage(60) ?: run {
                sendMessage("${player.name} 出牌超时,导致对局消失,大家快去骂他呀!")
                cancelGame()
                return
            }

            val content = nextMessage.message.contentToString()

            // 处理"过"的情况
            if (!isFirst && content.matches(Regex("过|不要|要不起"))) {
//                sendMessage("${player.name} 要不起,过!")
                player = game.nextPlayer
                continue
            }

            // 处理出牌的情况
            if (content.matches(Regex("^[0-9aAjJqQkK大小王炸]{1,20}$"))) {
                val listCar = parseCardInput(content)
                if (listCar.isEmpty()) {
                    sendMessage(player, "出牌格式错误，请按正确格式出牌！")
                    continue
                }

                if (!player.checkHand(listCar)) {
                    sendMessage(player, "你现在的手牌无法这样出!")
                    continue
                }

                val cards = listCar.toListCards()
                val match = CardFormUtil.match(cards)

                if (match == CardForm.ERROR) {
                    sendMessage(player, "你的出牌不符合规则,请重新出牌!")
                    continue
                }

                if (match == CardForm.BOMB || match == CardForm.GHOST_BOMB){
                    game.fold *= 2
                }

                // 检查牌型是否有效（如果是跟牌）
                if (!isFirst && !checkEat(maxForm, match, maxCards, cards)) {
                    sendMessage(player, "你要不起!")
                    continue
                }

                // 处理出牌后的公共操作
                handlePlay(player, cards, match, isFirst)

                // 检查是否获胜
                if (player.hand.isEmpty()) {
                    win = player
                    break
                } else {
                    player.sendMessage(player.toHand())
                }

                // 轮到下一位玩家
                player = game.nextPlayer
            } else {
                sendMessage(player, "出牌格式错误，只能包含[0-9]、A、J、Q、K、大小王等字符！")
            }
        }

        sendMessage("${win.name} 获胜!")

        if (game.landlord == win) {
            game.winPlayer.add(win)
        } else {
            game.winPlayer.addAll(game.players.filter { it != game.landlord })
        }

        stop()
    }

    /**
     * ->对局结束
     * ->计算倍率，计算积分，操作结果
     */
    override suspend fun stop() {
        status = GameStatus.STOP
        sendMessage("进入结算!")

        val winName = game.winPlayer.joinToString(",") { it.name }
        val integral = game.bottom * game.fold
        sendMessage("$winName 是赢家! 获得积分:$integral")

        players.forEach { group[it.id]?.modifyAdmin(false) }
        group.settings.isMuteAll = false
        cancelGame()
        sendMessage("游戏结束!")
    }

    //==游戏逻辑私有方法

    /**
     * 解析扑克牌输入字符串
     * 支持格式：10、J、Q、K、A、大王、小王等
     */
    private fun parseCardInput(input: String): List<Car> {
        val cards = mutableListOf<String>()
        val regex = Regex("10|大王|小王|王炸|[2-9aAjJqQkK]|0")
        var remaining = input

        while (remaining.isNotEmpty()) {
            val match = regex.find(remaining)
                ?: // 遇到无法解析的内容
                return emptyList()

            val card = match.value
            if (card == "王炸") {
                return listOf(Car.BIG_JOKER, Car.SMALL_JOKER)
            }
            cards.add(
                when (card) {
                    "0" -> "10"  // 将0转换为10
                    else -> card
                }
            )

            remaining = remaining.substring(card.length)
        }

        return cards.map { Car.fromMarking(it)!! }
    }

    /**
     * 检查吃不吃的起
     * @return true 吃的起
     */
    private fun checkEat(maxForm: CardForm, nowForm: CardForm, maxCards: List<Cards>, nowCards: List<Cards>): Boolean {
        //先判断炸弹
        when (maxForm.value) {
            2 -> if (nowForm.value == 1) return false
            3 -> if (nowForm.value == 1) return false
        }

        //如果牌型相等,则分别判断
        return if (maxForm == nowForm) {
            maxForm.check(maxCards, nowCards)
        } else {
            if (nowForm.value > maxForm.value) {
                nowForm == CardForm.BOMB || nowForm == CardForm.GHOST_BOMB
            } else {
                false
            }
        }
    }

    //==游戏过程辅助私有方法

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

    /**
     * 获取好友的下一条消息
     */
    suspend fun Player.nextMessage(timer: Int): MessageEvent? = nextMessage(this, timer)
}

