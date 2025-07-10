package cn.chahuyun.doudizhu.game

import cn.chahuyun.doudizhu.GameType
import cn.chahuyun.doudizhu.Player
import net.mamoe.mirai.contact.Group


/**
 * 一个游戏桌上的对局状态
 */
enum class GameStatus {
    /**
     * 刚开始
     */
    START,

    /**
     * 初始化
     */
    INITIAL,

    /**
     * 战斗中
     */
    BATTLE,

    /**
     * 结束
     */
    STOP
}

interface GameTable {
    /**
     * 群
     */
    val group: Group

    /**
     * 玩家
     */
    val players: List<Player>

    /**
     * 游戏类型
     */
    val gameType: GameType

    /**
     * ->游戏开始
     * ->检查好友，开启禁言，发牌
     * ->进入轮询消息监听，开始对局
     */
    suspend fun start()

    /**
     * ->游戏初期阶段
     * ->抢地主阶段
     * ->决定地主，补牌
     */
    suspend fun initial()

    /**
     * ->游戏对局阶段
     * ->出牌
     */
    suspend fun cards()

    /**
     * ->对局结束
     * ->计算倍率，计算积分，操作结果
     */
    suspend fun stop()
}




