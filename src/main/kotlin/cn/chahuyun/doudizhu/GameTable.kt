package cn.chahuyun.doudizhu

import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.MemberPermission


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
     * 对局信息
     */
    private val game: Game,
    /**
     * 群
     */
    private val group: Group,
    /**
     * bot
     */
    private val bot: Bot,
    /**
     * 游戏状态
     */
    private val status: Boolean,
    /**
     * 牌库
     */
    private var deck: List<Car> = Cards.createFullExpandDeck(),

    ) : GameTableProcess {

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

        game.players.forEach {
            if (bot.getFriend(it.id) == null) {
                sendMessage("${it.name} 还不是本${DZConfig.botName}的好友哦!")
                return
            }
        }


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
                game.players.forEach { at -> group[at.id]?.modifyAdmin(false) }
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

