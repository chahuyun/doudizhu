package cn.chahuyun.teafox.game.game


import cn.chahuyun.teafox.game.GameType
import cn.chahuyun.teafox.game.Player
import net.mamoe.mirai.contact.Group

class GandGameTable(
    override val group: Group,
    override val players: List<Player>,
    override val gameType: GameType
) : GameTable {

    /**
     * 游戏对局
     */
    lateinit var game: Game

    /**
     * ->游戏开始
     * ->检查好友，开启禁言，发牌
     * ->进入轮询消息监听，开始对局
     */
    override suspend fun start() {

    }

    /**
     * ->游戏初期阶段
     * ->抢地主阶段
     * ->决定地主，补牌
     */
    override suspend fun initial() {
        TODO("Not yet implemented")
    }

    /**
     * ->游戏对局阶段
     * ->出牌
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


}