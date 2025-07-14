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
import cn.chahuyun.teafox.game.util.GameTableUtil.asyncGetBottomScore
import cn.chahuyun.teafox.game.util.GameTableUtil.sendMessage
import cn.chahuyun.teafox.game.util.MessageUtil.nextMessage
import cn.chahuyun.teafox.game.util.MessageUtil.sendMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.Listener
import net.mamoe.mirai.event.events.FriendMessageEvent

/**
 * 对局
 */
data class Game(
    /**
     * 玩家
     */
    val players: List<Player>,
    /**
     * 群
     */
    val group: Long,
    /**
     * 底分
     */
    val bottom: Int,
    /**
     * 倍数
     */
    var fold: Int,
    /**
     * 剩余牌数
     */
    var residual: Int = 54,
    /**
     * 当前轮到哪个玩家出牌（索引）
     */
    var currentPlayerIndex: Int = 0,
    /**
     * 赢家
     */
    var winPlayer: MutableList<Player> = mutableListOf(),
) {

    /**
     * 地主
     */
    lateinit var landlord: Player

    /**
     * 给这个玩家加一张牌
     */
    fun addHand(playerIndex: Int, car: Card) {
        players[playerIndex].addHand(car)
    }

}

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
    /**
     * 游戏类型默认为 斗地主
     */
    override val gameType: GameType = GameType.DIZHU
) : CardGameTable(group, players, gameType), GameTable {


    /**
     * 消息转发玩家列表
     */
    private val forwardingPlayers = mutableSetOf<Player>()

    /**
     * 好友消息订阅映射表 (玩家ID -> 订阅对象)
     */
    private val friendSubscriptions = mutableMapOf<Long, Listener<FriendMessageEvent>>()

    /**
     * 当前玩家消息等待通道
     */
    private var currentPlayerChannel: Channel<String>? = null

    /**
     * 对局信息
     */
    private lateinit var game: Game

    /**
     * 底牌，先用空代替
     */
    private lateinit var bottomCards: List<Card>


    /**
     * 子类实现游戏流程管理
     * ->游戏开始
     * ->检查好友，开启禁言，发牌
     * ->进入轮询消息监听，开始对局
     */
    override suspend fun doStart() {
        val result = asyncGetBottomScore(type.min, type.max)

        result.onFailure { e ->
            when (e) {
                is TableFlipException -> sendMessage("${e.player.name} 掀桌(╯‵□′)╯︵┻━┻")
                is VotingTimeoutException -> sendMessage("配置超时,(╯‵□′)╯︵┻━┻")
                else -> {
                    sendMessage("配置过程中发生错误，游戏取消")
                    TeaFoxGames.error(e.message, e)
                }
            }
            cancelGame()
            return
        }

        // 获取成功结果
        val (finalBet, votes) = result.getOrThrow()

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
        var nextPlayer: Player = randomPlayer()

        var landlord: Player? = null
        // 记录每位玩家是否曾经抢过地主
        val qiang = players.associateWith { false }.toMutableMap()
        var round = 1
        var timer = 0
        while (true) {
            nextPlayer = nextPlayer(nextPlayer)
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
        var player = game.landlord
        // 比较的玩家
        var maxPlayer: Player? = null
        // 比较的手牌
        var maxCards: List<Card> = mutableListOf()
        // 比较的牌类型
        var maxForm: CardForm = CardForm.ERROR

        class WinHolder {
            var value: Player? = null
        }

        val winHolder = WinHolder()

        // 出牌计数器
        val cardsTimer = mutableMapOf<Player, Int>().apply {
            players.forEach { put(it, 0) }
        }

        //上一个响应指令的玩家
        var previousPlayer: Player? = null

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
                sendMessageForModel(msg)
                maxPlayer = player
                maxForm = match
                maxCards = cards
                cardsTimer[player] = (cardsTimer[player] ?: 0) + 1
            } else {
                sendMessage("错误!")
            }
        }

        /**
         * 出牌
         * @param player 玩家
         * @param content 内容
         * @return Pair<Boolean,Boolean> 是(true)否获胜,是(true)否出牌
         */
        suspend fun handleCardPlay(player: Player, content: String, isFirst: Boolean): Pair<Boolean, Boolean> {
            val cards = parseCardInput(content).toCard()
            val forwardingMode = player in forwardingPlayers

            if (cards.isEmpty()) {
                val msg = "出牌格式错误，请按正确格式出牌！"
                if (forwardingMode) {
                    player.sendMessage(msg)
                } else {
                    sendMessage(player, msg)
                }
                return false to false
            }

            if (!player.canPlayCards(cards)) {
                val msg = "你现在的手牌无法这样出!"
                if (forwardingMode) {
                    player.sendMessage(msg)
                } else {
                    sendMessage(player, msg)
                }
                return false to false
            }

            val match = CardFormUtil.match(cards.toGroup())

            if (match == CardForm.ERROR) {
                val msg = "你的出牌不符合规则,请重新出牌!"
                if (forwardingMode) {
                    player.sendMessage(msg)
                } else {
                    sendMessage(player, msg)
                }
                return false to false
            }

            if (match == CardForm.BOMB || match == CardForm.GHOST_BOMB) {
                game.fold *= 2
            }

            // 检查牌型是否有效（如果是跟牌）
            if (!isFirst && !checkEat(maxForm, match, maxCards, cards)) {
                val msg = "你要不起!"
                if (forwardingMode) {
                    player.sendMessage(msg)
                } else {
                    sendMessage(player, msg)
                }
                return false to false
            }

            // 处理出牌后的公共操作
            handlePlay(player, cards, match, isFirst)

            // 检查是否获胜
            if (player.hand.isEmpty()) {
                winHolder.value = player
                return true to true
            } else {
                player.sendMessage(player.toHand())
                return (false to true)
            }
        }

        while (true) {
            //第一次出牌或者轮到他出牌
            val isFirst = maxPlayer == null || maxPlayer == player

            // 通知玩家出牌
            notifyPlayerTurn(player, isFirst, previousPlayer)

            // 获取玩家响应，需要对超时处理！
            val response = waitForPlayerResponse(player) ?: run {
                abnormalEnd(player)
                stopGame()
                return
            }

            // 处理玩家响应
            when {
                response == "启用转发" -> {
                    enableMessageForwarding(player)
                }

                response == "禁用转发" -> {
                    disableMessageForwarding(player)
                }

                isSkipResponse(response) -> {
                    // 只有在不是第一位出牌者时才能过
                    if (!isFirst) {
                        val msg = "${player.name} 不要"
                        sendMessageForModel(msg)
                        previousPlayer = player
                        player = nextPlayer(player)
                        continue
                    }
                }

                isValidCardResponse(response) -> {
                    // 处理出牌并检查是否获胜
                    val (isWin, isCard) = handleCardPlay(player, response, isFirst)
                    if (isWin) break // 如果获胜则跳出循环
                    if (!isCard) continue

                    // 未获胜则轮到下一位玩家
                    player = nextPlayer(player)
                    continue
                }

                else -> {}
            }
            previousPlayer = player
            continue
        }

        val winPlayer = winHolder.value

        if (winPlayer == null) {
            sendMessage("游戏错误!")
            stopGame()
            return
        }

        if (cardsTimer.filter { it.key != winPlayer }.map { it.value }.sum() == 0) {
            sendMessage("${winPlayer.name} 春天!翻4倍!!")
            game.fold *= 4
        } else {
            sendMessage("${winPlayer.name} 获胜!")
        }

        if (game.landlord == winPlayer) {
            game.winPlayer.add(winPlayer)
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

    // 通知玩家轮到出牌
    private suspend fun notifyPlayerTurn(player: Player, isFirst: Boolean, previousPlayer: Player?) {
        val prompt = "请出牌!" + if (isFirst) "" else "或者过"

        //只有在第一次轮到该用户且有玩家在群内进行游戏时，才在群内通知
        val inForwarding = isForwardingEnabled(player)
        if (inForwarding) {
            if (previousPlayer == null || previousPlayer != player) {
                sendMessage("${player.name} $prompt")
                delay(200)
                player.sendMessage("$prompt ${player.toHand()}")
            } else {
                player.sendMessage("$prompt \n ${player.toHand()}")
                delay(200)
            }
        } else {
            sendMessage(player, prompt)
        }
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
        nowCards: List<Card>,
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

    /**
     * 判断是否为跳过出牌
     */
    private fun isSkipResponse(content: String) = content.matches(Regex("\\.{2,4}|go?|过|不要|要不起"))

    /**
     * 判断是否为有效牌
     */
    private fun isValidCardResponse(content: String) = content.matches(Regex("^[0-9aAjJqQkK大小王炸]{1,20}$"))

    //==游戏过程辅助私有方法

    /**
     * 启用消息转发模式
     */
    private suspend fun enableMessageForwarding(player: Player) {
        if (player !in forwardingPlayers) {
            forwardingPlayers.add(player)
            sendMessage("${player.name} 已启用消息转发模式")
            player.sendMessage("已启用消息转发，请通过好友消息出牌")
        }
    }

    // 禁用消息转发
    private suspend fun disableMessageForwarding(player: Player) {
        if (player in forwardingPlayers) {
            forwardingPlayers.remove(player)
            sendMessage("${player.name} 已禁用消息转发模式")
            player.sendMessage("已禁用消息转发，请通过群消息出牌")
        }
    }

    // 检查转发状态时使用ID
    private fun isForwardingEnabled(player: Player) = player in forwardingPlayers

    // 广播必要消息
    private suspend fun sendMessageForModel(message: String) {
        sendMessage(message)
        delay(200)
        forwardingPlayers.forEach {
            it.sendMessage(message)
            delay(50)
        }
    }

    // 等待玩家响应
    private suspend fun waitForPlayerResponse(player: Player): String? {
        // 创建响应通道
        val responseChannel = Channel<String>(1)
        currentPlayerChannel = responseChannel

        return try {
            val b = isForwardingEnabled(player)
//            debug("${player.name} 是否存在 $b")
            withTimeout(DZConfig.timeOut * 1000L) {
                if (b) {
                    // 异步监听好友消息
                    setupFriendMessageListener(player, responseChannel)
                    responseChannel.receive()
                } else {
                    // 同步等待群消息
                    player.nextMessage(group, DZConfig.timeOut)?.contentToString()
                }
            }
        } catch (_: TimeoutCancellationException) {
            abnormalEnd(player)
            null
        } finally {
            // 清理当前监听器
            currentPlayerChannel = null
            friendSubscriptions[player.id]?.complete()
            friendSubscriptions.remove(player.id)
        }
    }

    // 设置好友消息监听器
    private fun setupFriendMessageListener(player: Player, channel: Channel<String>) {
        // 取消可能存在的旧订阅
        friendSubscriptions[player.id]?.complete()

        // 创建新订阅
        val subscription = TeaFoxGames.channel
            .filter { it.sender.id == player.id }
            .subscribeAlways<FriendMessageEvent> { event ->
                val content = event.message.contentToString()

                // 只处理游戏相关指令
                if (content == "禁用消息转发模式" ||
                    isSkipResponse(content) ||
                    isValidCardResponse(content)
                ) {

                    // 在协程上下文中发送响应
                    CoroutineScope(Dispatchers.Default).launch {
                        channel.send(content)
                    }
                }
            }

        friendSubscriptions[player.id] = subscription
    }

    private suspend fun stopGame() {
        players.forEach { group[it.id]?.modifyAdmin(false) }
        group.settings.isMuteAll = false
        cleanup()
        cancelGame()
    }

    // 清理资源
    private fun cleanup() {
        // 取消所有监听
        friendSubscriptions.values.forEach { it.complete() }
        friendSubscriptions.clear()

        // 关闭通道
        currentPlayerChannel?.close()
        currentPlayerChannel = null
    }


}