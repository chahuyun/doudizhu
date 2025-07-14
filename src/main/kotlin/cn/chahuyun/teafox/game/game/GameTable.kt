package cn.chahuyun.teafox.game.game

import cn.chahuyun.teafox.game.GameEvent
import cn.chahuyun.teafox.game.GameType
import cn.chahuyun.teafox.game.Player
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

    /**
     * 取消游戏
     */
    fun cancelGame() {
        GameEvent.cancelGame(group)
    }

    /**
     * 获取下一个玩家
     * @param currentPlayer 当前玩家
     * @return 下一个玩家
     */
    fun nextPlayer(currentPlayer: Player? = null): Player {
        if (players.isEmpty()) throw IllegalStateException("玩家列表为空")

        val index = if (currentPlayer == null) -1 else players.indexOf(currentPlayer)
        if (index != -1 && index !in players.indices) {
            throw IllegalArgumentException("当前玩家不在玩家列表中")
        }

        // 如果没有当前玩家，从第一个开始
        return players[(index + 1) % players.size]
    }

    /**
     * 随机一个玩家
     * @return 随机玩家
     */
    fun randomPlayer(): Player {
        return players.random()
    }

}


/**
 * 掀桌异常
 */
class TableFlipException(val player: Player) : RuntimeException()

/**
 * 超时异常
 */
class VotingTimeoutException : RuntimeException()

