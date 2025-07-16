package cn.chahuyun.teafox.game.game


import cn.chahuyun.teafox.game.*
import cn.chahuyun.teafox.game.util.CardUtil
import cn.chahuyun.teafox.game.util.GameTableUtil.asyncGetBottomScore
import cn.chahuyun.teafox.game.util.GameTableUtil.sendMessage
import cn.chahuyun.teafox.game.util.MessageUtil.nextMessage
import net.mamoe.mirai.contact.Group

class GandGameTable(
    override val group: Group,
    override val players: List<Player>,
    override val gameType: GameType = GameType.GAND
) : CardGameTable(group, players, gameType), GameTable {

    /**
     * 游戏对局
     */
    lateinit var game: Game

    var cardPile = mutableListOf<Card>()

    lateinit var nextPlayer: Player

    /**
     * 子类实现游戏流程管理
     */
    override suspend fun doStart() {
        val result = asyncGetBottomScore(100, 1000)

        result.onFailure { e ->
            handleGameException(e)
            cancelGame()
            return
        }

        // 获取成功结果
        val (finalBet, votes) = result.getOrThrow()

        sendMessage("三人输入底分分别为：${votes.values.joinToString(",")}，最终底分为：$finalBet")
        group.settings.isMuteAll = true
        initial()
    }


    /**
     * ->游戏初期阶段
     * ->抢地主阶段
     * ->决定地主，补牌
     */
    override suspend fun initial() {
        cardPile.addAll(CardUtil.createFullExpandDeck())
        nextPlayer = randomPlayer()

        while (!players.all { it.hand.size == 5 }) {
            nextPlayer.addHand(cardPile.removeFirst())
            nextPlayer = nextPlayer(nextPlayer)
        }

        nextPlayer.addHand(cardPile.removeFirst())
        players.forEach { "你的手牌:${it.toHand()}" }
        cards()
    }

    /**
     * ->游戏对局阶段
     * ->出牌
     */
    override suspend fun cards() {
        //上一个出牌
        var previousCards: List<Card>
        //上一个出牌玩家
        var previousPlayer: Player
        //出牌计数器
        val cardCounter = buildMap {
            players.forEach { put(it, 0) }
        }

        while (true) {
            val message =  nextPlayer.nextMessage(group, DZConfig.timeOut)?.contentToString() ?: run {
                abnormalEnd(nextPlayer)
                stopGame()
                return
            }
        }

    }

    /**
     * ->对局结束
     * ->计算倍率，计算积分，操作结果
     */
    override suspend fun stop() {
        TODO("Not yet implemented")
    }


    //==游戏逻辑私有方法

    /**
     * 游戏阶段异常结束
     */
    private suspend fun abnormalEnd(player: Player) {
        val totalCoins = game.bottom
        val validPlayers = players.filter { it != player }.toMutableList()


        val size = validPlayers.size
        val half = totalCoins / size
        val remainder = totalCoins % size

        val player1 = validPlayers.removeFirst()

        val foxUser1 = FoxUserManager.getFoxUser(player1)

        foxUser1.addCoins(half + remainder)
        validPlayers.forEach {
            FoxUserManager.getFoxUser(it).addCoins(half)
        }

        sendMessage(
            """
            ${player.name} 出牌超时,导致对局消失,扣除${game.bottom}狐币给其他两位玩家!
            ${player1.name} 获得 ${half + remainder} 狐币!
            ${validPlayers.joinToString(",") { it.name }} 获得 $half 狐币!
            另外,大家快去骂 ${player.name} 呀!
        """.trimIndent()
        )
        stopGame()
    }


    private fun stopGame() {
        group.settings.isMuteAll = false
        cancelGame()
    }
}