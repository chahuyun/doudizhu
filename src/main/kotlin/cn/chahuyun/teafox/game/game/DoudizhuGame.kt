package cn.chahuyun.teafox.game.game

import cn.chahuyun.teafox.game.*
import cn.chahuyun.teafox.game.FoxUserManager.addLose
import cn.chahuyun.teafox.game.FoxUserManager.addVictory
import cn.chahuyun.teafox.game.game.CardFormUtil.check
import cn.chahuyun.teafox.game.util.CardUtil
import cn.chahuyun.teafox.game.util.CardUtil.containsRank
import cn.chahuyun.teafox.game.util.CardUtil.getRankSize
import cn.chahuyun.teafox.game.util.CardUtil.knowCardRank
import cn.chahuyun.teafox.game.util.CardUtil.show
import cn.chahuyun.teafox.game.util.CardUtil.toCard
import cn.chahuyun.teafox.game.util.CardUtil.toGroup
import cn.chahuyun.teafox.game.util.FriendUtil
import cn.chahuyun.teafox.game.util.GameTableUtil.sendMessage
import cn.chahuyun.teafox.game.util.MessageUtil.nextMessage
import cn.chahuyun.teafox.game.util.MessageUtil.sendMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.MemberPermission
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.buildMessageChain
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * 游戏桌
 */
@Suppress("SpellCheckingInspection")
class DizhuGameTable(
    /**
     * 群
     */
    override val group: Group,
    /**
     * 玩家
     */
    override val players: List<Player>,
    /**
     * bot
     */
    private val bot: Bot,
    /**
     * 游戏状态
     */
    private var status: GameStatus = GameStatus.START,
    /**
     * 牌库
     */
    private val deck: List<Card> = CardUtil.createFullExpandDeck(),
    /**
     * 默认对局类型
     */
    private val type: GameTableCoinsType = GameTableCoinsType.NORMAL,
) : GameTable {
    /**
     * 游戏类型默认为 斗地主
     */
    override val gameType: GameType = GameType.DIZHU

    /**
     * 对局信息
     */
    private lateinit var game: Game

    /**
     * 底牌，先用空代替
     */
    private lateinit var bottomCards: List<Card>

    /**
     * 掀桌异常
     */
    private inner class TableFlipException(val player: Player) : RuntimeException()
    private inner class VotingTimeoutException : RuntimeException()

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

        withTimeoutOrNull(120 * 1000L) {
            val util = FriendUtil(players)
            if (util.check()) return@withTimeoutOrNull
            else sendMessage("游戏开始,请在2分钟内添加本bot的好友!")
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

        val votes = ConcurrentHashMap<Long, Int>() // 线程安全的Map

        try {
            coroutineScope {
                sendMessage(buildMessageChain {
                    players.forEach { player ->
                        +At(player.id)
                    }
                    +"请同时投票底分(${type.min}~${type.max})，发送「掀桌」停止配置"
                })

                // 为每个玩家启动异步任务
                players.map { player ->
                    async {
                        try {
                            val input = player.nextMessage(group, DZConfig.timeOut)?.contentToString()?.trim()
                                ?: throw VotingTimeoutException()

                            if (input.startsWith("掀桌")) {
                                throw TableFlipException(player)
                            }

                            val bet = input.toIntOrNull()
                            if (bet != null && bet in type.min..type.max) {
                                votes[player.id] = bet
                                sendMessage("${player.name} 配置底分为: $bet!")
                            } else {
                                sendMessage("${player.name} 输入无效，底分范围为 ${type.min}~${type.max}，已使用默认值 ${type.min}。")
                                votes[player.id] = type.min
                            }
                        } catch (e: Exception) {
                            when (e) {
                                is TableFlipException, is VotingTimeoutException -> throw e
                                else -> {
                                    // 其他异常使用默认值
                                    votes[player.id] = type.min
                                }
                            }
                        }
                    }
                }.awaitAll()
            }
        } catch (e: TableFlipException) {
            sendMessage("${e.player.name} 掀桌(╯‵□′)╯︵┻━┻")
            cancelGame()
            return
        } catch (_: VotingTimeoutException) {
            sendMessage("配置超时,(╯‵□′)╯︵┻━┻")
            cancelGame()
            return
        } catch (e: Exception) {
            sendMessage("配置过程中发生错误，游戏取消")
            cancelGame()
            TeaFoxGames.error(e.message, e)
            return
        }

        // 计算平均值
        val total = votes.values.sum()
        // 平均值
        val average = total / votes.size.toDouble()
        // 向下取整
        val finalBet = floor(average).roundToInt()

        sendMessage("三人输入底分分别为：${votes.values.joinToString(",")}，最终底分为：$finalBet")

        game = Game(players, group.id, finalBet, 1)

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

        initial()
    }

    /**
     * ->抢地主阶段
     * ->决定地主，补牌
     */
    override suspend fun initial() {
        status = GameStatus.INITIAL
        var nextPlayer: Player

        var landlord: Player? = null
        // 记录每位玩家是否曾经抢过地主
        val qiang = players.associateWith { false }.toMutableMap()
        var round = 1
        var timer = 0
        val shuffledPlayers  = players.shuffled()
        while (true) {
            nextPlayer = shuffledPlayers[timer % 3]
            if (round == 1) {
                //第一轮
                sendMessage(nextPlayer.id, "开始抢地主:(抢/抢地主)")
                val content = nextPlayer.nextMessage(group, DZConfig.timeOut)?.contentToString() ?: run {
                    sendMessage("发送超时,(╯‵□′)╯︵┻━┻")
                    stopGame()
                    return
                }

                if (content.matches(Regex("^q|抢|抢地主"))) {
                    landlord = nextPlayer
                    qiang[nextPlayer] = true
                    sendMessage("${nextPlayer.name} 抢地主！")
                } else {
                    sendMessage("${nextPlayer.name} 不抢。")
                }
                timer++
                if (timer == 3) {
                    //三人操作完成,验证后进行第二轮
                    val stillBidding = qiang.filterValues { it }.keys.toList()
                    if (stillBidding.size == 1) {
                        break
                    } else if (stillBidding.isEmpty()) {
                        // TODO: 可以后期再决定是否重新发牌/重置游戏
                        landlord = players.random()
                        sendMessage("无人角逐地主,那本${DZConfig.botName}就随便指定一个了!恭喜${landlord.name}成功地主!")
                        break
                    } else {
                        round = 2
                        continue
                    }
                }
            }

            //第二轮
            if (round == 2) {
                if (qiang[nextPlayer]!!) {
                    sendMessage(nextPlayer.id, "开始角逐抢地主:(抢/抢地主)")
                    val content = nextPlayer.nextMessage(group, DZConfig.timeOut)?.contentToString() ?: run {
                        sendMessage("发送超时,(╯‵□′)╯︵┻━┻")
                        stopGame()
                        return
                    }

                    if (content.matches(Regex("^q|抢|抢地主"))) {
                        landlord = nextPlayer
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

        if (bottomCards.getRankSize() == 1) {
            sendMessage("底牌是3连:${bottomCards.show()},翻倍!")
            game.fold *= 2
        } else if (bottomCards.containsRank(CardRank.SMALL_JOKER) && bottomCards.containsRank(CardRank.BIG_JOKER)) {
            sendMessage("底牌是王炸:${bottomCards.show()},翻倍!")
            game.fold *= 2
        } else {
            sendMessage("底牌是:${bottomCards.show()}")
        }

        game.landlord = landlord
        game.setNextPlayer(landlord)
        landlord.sendMessage("你的手牌:\n" + " ${landlord.toHand()}")
        if (round == 2) {
            game.fold *= 2
            sendMessage("${landlord.name} 角逐胜者,翻倍!请开始出牌!")
        } else {
            sendMessage("${landlord.name} 抢到了地主!请开始出牌!")
        }

        cards()
    }

    /**
     * ->出牌
     */
    @Suppress("DuplicatedCode")
    override suspend fun cards() {
        status = GameStatus.BATTLE
        var player = game.nextPlayer

        // 比较的玩家
        var maxPlayer: Player? = null
        // 比较的手牌
        var maxCards: List<Card> = mutableListOf()
        // 比较的牌类型
        var maxForm: CardForm = CardForm.ERROR
        val win: Player
        // 出牌计数器
        val cardsTimer = mutableMapOf<Player, Int>().apply {
            players.forEach { put(it, 0) }
        }

        // 提取出牌后的公共操作
        suspend fun handlePlay(player: Player, cards: List<Card>, match: CardForm, isFirst: Boolean) {
            val action = if (isFirst) "出牌" else "管上"
            var msg = when (match) {
                CardForm.BOMB -> "${player.name} : 炸弹(翻倍)! ${cards.show()}"
                CardForm.TRIPLE_ONE -> "${player.name} : 三带一! ${cards.show()}"
                CardForm.TRIPLE_TWO -> "${player.name} : 三带二! ${cards.show()}"
                CardForm.QUEUE -> "${player.name} : 顺子! ${cards.show()}"
                CardForm.GHOST_BOMB -> "${player.name} : 王炸(翻倍)!!!"
                CardForm.QUEUE_TWO -> "${player.name} : 连对! ${cards.show()}"
                CardForm.AIRCRAFT -> "${player.name} : 飞机! ${cards.show()}"
                CardForm.AIRCRAFT_SINGLE -> "${player.name} : 飞机! ${cards.show()}"
                CardForm.AIRCRAFT_PAIR -> "${player.name} : 飞机! ${cards.show()}"
                else -> "${player.name} : $action ${cards.show()}"
            }

            if (player.playCards(cards)) {
                val handSize = player.hand.size
                if (handSize in 1..3) {
                    msg = msg.plus("\n${player.name} 只剩 $handSize 张牌了!")
                }

                sendMessage(msg)
                maxPlayer = player
                maxForm = match
                maxCards = cards
                cardsTimer[player] = (cardsTimer[player] ?: 0) + 1
            } else {
                sendMessage("错误!")
            }
        }

        while (true) {
            val isFirst = maxPlayer == null || maxPlayer == player
            sendMessage(player, "请出牌!" + if (isFirst) "" else "或者过")

            val content = player.nextMessage(DZConfig.timeOut)?.contentToString() ?: run {
                abnormalEnd(player)
                return
            }

            // 处理"过"的情况
            if (!isFirst && content.matches(Regex("\\.{2,4}|go?|过|不要|要不起"))) {
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

                if (!player.canPlayCards(listCar)) {
                    sendMessage(player, "你现在的手牌无法这样出!")
                    continue
                }

                val cards = listCar.toCard()
                val match = CardFormUtil.match(cards.toGroup())

                if (match == CardForm.ERROR) {
                    sendMessage(player, "你的出牌不符合规则,请重新出牌!")
                    continue
                }

                if (match == CardForm.BOMB || match == CardForm.GHOST_BOMB) {
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

        if (cardsTimer.filter { it.key != win }.map { it.value }.sum() == 0) {
            sendMessage("${win.name} 春天!翻4倍!!")
            game.fold *= 4
        } else {
            sendMessage("${win.name} 获胜!")
        }

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

        val winPlayer = game.winPlayer
        val losePlayer = players.filter { it !in winPlayer }

        val resutl = integral / 2
        val surplus = integral % 2

        if (winPlayer.size == 1) {
            sendMessage("$winName 是赢家! 获得狐币: $integral !")
            require(losePlayer.size == 2)
            val (p1, p2) = losePlayer
            FoxUserManager.getFoxUser(winPlayer.first()).addVictory(integral, true)
            FoxUserManager.getFoxUser(p1).addLose(resutl + surplus)
            FoxUserManager.getFoxUser(p2).addLose(resutl)
        } else {
            sendMessage("$winName 是赢家! 分别获得狐币: $resutl !")
            require(winPlayer.size == 2)
            val (p1, p2) = winPlayer
            FoxUserManager.getFoxUser(p1).addVictory(resutl + surplus)
            FoxUserManager.getFoxUser(p2).addVictory(resutl)
            FoxUserManager.getFoxUser(losePlayer.first()).addLose(integral, true)
        }

        stopGame()
        sendMessage("游戏结束!")
    }

    //==游戏逻辑私有方法

    /**
     * 游戏阶段异常结束
     */
    private suspend fun abnormalEnd(player: Player) {
        val totalCoins = game.bottom
        val validPlayers = players.filter { it != player }

        require(validPlayers.size == 2) { "异常：应该有且仅有两个其他玩家！" }

        val (p1, p2) = validPlayers
        val half = totalCoins / 2
        val remainder = totalCoins % 2

        val p1User = FoxUserManager.getFoxUser(p1)
        val p2User = FoxUserManager.getFoxUser(p2)

        // 扣除超时玩家金币并分配
        val timeoutUser = FoxUserManager.getFoxUser(player)
        timeoutUser.minusCoins(totalCoins)
        p1User.addCoins(half + remainder)
        p2User.addCoins(half)

        // 胜负判定
        if (game.landlord == player) {
            // 超时的是地主 → 农民胜利
            timeoutUser.addLose(0, true)
            p1User.addVictory(0)
            p2User.addVictory(0)
        } else {
            // 超时的是农民 → 地主胜利
            timeoutUser.addLose(0)

            val landlordPlayer = validPlayers.find { it == game.landlord }!!
            val otherPlayer = validPlayers.find { it != landlordPlayer }!!

            val landlordUser = FoxUserManager.getFoxUser(landlordPlayer)
            val otherUser = FoxUserManager.getFoxUser(otherPlayer)

            landlordUser.addVictory(0, true)
            otherUser.addLose(0)
        }

        sendMessage(
            """
            ${player.name} 出牌超时,导致对局消失,扣除${game.bottom}狐币给其他两位玩家!
            ${p1.name} 获得 ${half + remainder} 狐币!
            ${p2.name} 获得 $half 狐币!
            另外,大家快去骂 ${player.name} 呀!
        """.trimIndent()
        )
        stopGame()
    }

    /**
     * 解析扑克牌输入字符串
     * 支持格式：10、J、Q、K、A、大王、小王等
     */
    private fun parseCardInput(input: String): List<CardRank> {
        val cards = mutableListOf<String>()
        val regex = Regex("10|大王|小王|王炸|[2-9aAjJqQkK]|0")
        var remaining = input

        while (remaining.isNotEmpty()) {
            val match = regex.find(remaining)
                ?: // 遇到无法解析的内容
                return emptyList()

            val card = match.value
            if (card == "王炸") {
                return listOf(CardRank.BIG_JOKER, CardRank.SMALL_JOKER)
            }
            cards.add(
                when (card) {
                    "0" -> "10"  // 将0转换为10
                    else -> card
                }
            )

            remaining = remaining.substring(card.length)
        }

        return cards.map { knowCardRank(it) }
    }

    /**
     * 检查吃不吃的起
     * @return true 吃的起
     */
    private fun checkEat(
        maxForm: CardForm,
        nowForm: CardForm,
        maxCards: List<Card>,
        nowCards: List<Card>
    ): Boolean {
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


    private suspend fun GameTable.stopGame() {
        players.forEach { group[it.id]?.modifyAdmin(false) }
        group.settings.isMuteAll = false
        cancelGame()
    }
}