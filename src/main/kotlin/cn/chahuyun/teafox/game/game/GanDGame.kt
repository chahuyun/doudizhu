package cn.chahuyun.teafox.game.game


import cn.chahuyun.teafox.game.GameType
import cn.chahuyun.teafox.game.Player
import cn.chahuyun.teafox.game.TeaFoxGames
import cn.chahuyun.teafox.game.util.GameTableUtil.asyncGetBottomScore
import cn.chahuyun.teafox.game.util.GameTableUtil.sendMessage
import net.mamoe.mirai.contact.Group

class GandGameTable(
    override val group: Group,
    override val players: List<Player>,
    override val gameType: GameType = GameType.GAND
) : CardGameTable(group, players, gameType),GameTable {

    /**
     * 游戏对局
     */
    lateinit var game: Game

    /**
     * 子类实现游戏流程管理
     */
    override suspend fun doStart() {
        val result = asyncGetBottomScore(100,1000)

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