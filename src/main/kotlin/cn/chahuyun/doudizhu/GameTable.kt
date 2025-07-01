package cn.chahuyun.doudizhu

import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.MemberPermission

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
            return
        }

        players.forEach {
            if (bot.getFriend(it.id) == null) {
                sendMessage("${it.name} 还不是本${DZConfig.botName}的好友哦!")
                return
            }
        }

        game = Game(players, group.id, 3, 0, players[0])

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
}

