package cn.chahuyun.teafox.game.game

import cn.chahuyun.teafox.game.DZConfig
import cn.chahuyun.teafox.game.GameType
import cn.chahuyun.teafox.game.Player
import cn.chahuyun.teafox.game.TeaFoxGames
import cn.chahuyun.teafox.game.util.FriendUtil
import cn.chahuyun.teafox.game.util.GameTableUtil.sendMessage
import kotlinx.coroutines.withTimeoutOrNull
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.MemberPermission

/**
 * 棋牌类游戏的基类
 */
abstract class CardGameTable(
    /**
     * 群
     */
    override val group: Group,
    /**
     * 玩家
     */
    override val players: List<Player>,
    /**
     * 游戏类型
     */
    override val gameType: GameType,

    ) : GameTable {
    /**
     * 机器人
     */
    val bot: Bot = TeaFoxGames.bot

    /**
     * 标记游戏是否已取消
     */
    protected var gameCancelled = false



    /**
     * ->游戏开始
     * ->检查好友，开启禁言，发牌
     * ->进入轮询消息监听，开始对局
     */
    final override suspend fun start() {
        // 前置检查
        if (!checkGameEnv()) {
            cancelGame()
            return
        }

        // 只有检查通过才执行游戏流程
        if (!gameCancelled) {
            doStart()
        }
    }

    /**
     * 子类实现游戏流程管理
     */
    protected abstract suspend fun doStart()

    /**
     * ->游戏初期阶段
     * ->抢地主阶段
     * ->决定地主，补牌
     */
    abstract override suspend fun initial()

    /**
     * ->游戏对局阶段
     * ->出牌
     */
    abstract override suspend fun cards()

    /**
     * ->对局结束
     * ->计算倍率，计算积分，操作结果
     */
    abstract override suspend fun stop()

    /**
     * 检查游戏环境
     */
    suspend fun checkGameEnv(): Boolean {
        if (group.botPermission != MemberPermission.OWNER) {
            sendMessage("本${DZConfig.botName}不是群主哦~")
            cancelGame()
            return false
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
                    return false
                }
            }
        }
        return true
    }

    /**
     * 响应报错信息
     */
    protected suspend fun handleGameException(e: Throwable) {
        when (e) {
            is TableFlipException -> sendMessage("${e.player.name} 掀桌(╯‵□′)╯︵┻━┻")
            is VotingTimeoutException -> sendMessage("配置超时,(╯‵□′)╯︵┻━┻")
            else -> {
                sendMessage("配置过程中发生错误，游戏取消")
                TeaFoxGames.error(e.message, e)
            }
        }
    }
}