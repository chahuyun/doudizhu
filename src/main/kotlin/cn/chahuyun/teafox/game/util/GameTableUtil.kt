package cn.chahuyun.teafox.game.util

import cn.chahuyun.teafox.game.DZConfig
import cn.chahuyun.teafox.game.Player
import cn.chahuyun.teafox.game.TeaFoxGames
import cn.chahuyun.teafox.game.game.GameTable
import cn.chahuyun.teafox.game.game.TableFlipException
import cn.chahuyun.teafox.game.game.VotingTimeoutException
import cn.chahuyun.teafox.game.util.MessageUtil.nextMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.buildMessageChain
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set
import kotlin.math.floor
import kotlin.math.roundToInt


/**
 * 游戏桌工具
 */
object GameTableUtil {

    /**
     * 向游戏桌所在的群组发送一条纯文本消息
     * 
     * @param msg 要发送的字符串消息内容
     */
    suspend fun GameTable.sendMessage(msg: String) {
        this.group.sendMessage(msg)
    }

    /**
     * 向游戏桌所在的群组发送一条结构化消息链
     * 
     * @param msg 要发送的消息链对象
     */
    suspend fun GameTable.sendMessage(msg: MessageChain) {
        this.group.sendMessage(msg)
    }

    /**
     * 向指定QQ用户发送@消息（使用ID）
     * 
     * @param id  目标用户的QQ号码
     * @param msg 要发送的字符串消息内容
     */
    suspend fun GameTable.sendMessage(id: Long, msg: String) {
        this.group.sendMessage(At(id).plus(PlainText(msg)))
    }

    /**
     * 向指定玩家发送@消息
     * 
     * @param player 目标玩家对象
     * @param msg    要发送的字符串消息内容
     */
    suspend fun GameTable.sendMessage(player: Player, msg: String) {
        this.group.sendMessage(At(player.id).plus(PlainText(msg)))
    }

    /**
     * 异步获取玩家投票的底分
     * @param min 最小值
     * @param max 最大值
     * @return Pair<Int, Map<Long, Int>>
     */
    suspend fun GameTable.asyncGetBottomScore(min: Int, max: Int): Result<Pair<Int, Map<Long, Int>>> {
        val votes = ConcurrentHashMap<Long, Int>()

        return try {
            coroutineScope {
                sendMessage(buildMessageChain {
                    players.forEach { player ->
                        +At(player.id)
                    }
                    +"请同时投票底分(${min}~${max})，发送「掀桌」停止配置"
                })

                players.map { player ->
                    async {
                        try {
                            val input = player.nextMessage(group, DZConfig.timeOut)?.contentToString()?.trim()
                                ?: throw VotingTimeoutException()

                            if (input.startsWith("掀桌")) {
                                throw TableFlipException(player)
                            }

                            val bet = input.toIntOrNull()
                            if (bet != null && bet in min..max) {
                                votes[player.id] = bet
                                sendMessage("${player.name} 配置底分为: $bet!")
                            } else {
                                sendMessage("${player.name} 输入无效，底分范围为 ${min}~${max}，已使用默认值 ${min}。")
                                votes[player.id] = min
                            }
                        } catch (e: Exception) {
                            when (e) {
                                is TableFlipException, is VotingTimeoutException -> throw e
                                else -> votes[player.id] = min
                            }
                        }
                    }
                }.awaitAll()
            }

            // 计算平均值
            val total = votes.values.sum()
            val average = total / votes.size.toDouble()
            val finalBet = floor(average).roundToInt()

            Result.success(finalBet to votes.toMap())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}