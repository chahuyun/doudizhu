package cn.chahuyun.teafox.game.util

import cn.chahuyun.teafox.game.Player
import cn.chahuyun.teafox.game.TeaFoxGames
import cn.chahuyun.teafox.game.TeaFoxGames.bot
import cn.chahuyun.teafox.game.util.MessageUtil.nextMessage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageChain

/**
 * 消息事件监听工具类，提供异步等待指定用户或群组发送消息的功能。
 *
 * 该工具类基于 `mirai` 的事件通道与 Kotlin 协程实现，支持：
 * - 同步/异步获取下一条消息
 * - 带超时机制的消息监听
 * - 针对玩家（[Player]）群组（[Group]）的封装
 *
 * 所有方法均挂起执行，适用于协程环境。
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
object MessageUtil {

    //== 接受消息的方法工具 ==

    /**
     * 获取玩家的下一条消息,不区分环境,默认30秒超时
     * 优先使用此方法
     *
     * @see MessageUtil.nextMessage
     */
    @JvmName("nextPlayerMessage")
    suspend fun Player.nextMessage(): MessageChain? = nextMessage(this, 30)

    /**
     * 获取玩家的下一条消息,不区分环境
     * 优先使用此方法
     *
     * @param timer 超时时间(秒)
     * @see MessageUtil.nextMessage
     */
    @JvmName("nextPlayerMessageTimer")
    suspend fun Player.nextMessage(timer: Int): MessageChain? = nextMessage(this, timer)

    /**
     * 获取玩家在这个群的下一条消息,默认30秒超时
     * 优先使用此方法
     *
     * @param group 要监听的群组对象
     * @see MessageUtil.nextMessage
     */
    suspend fun Player.nextMessage(group: Group): MessageChain? =
        nextInGroupMessage(this, group, 30)?.message


    /**
     * 获取玩家在这个群的下一条消息
     * 优先使用此方法
     *
     * @param group 要监听的群组对象
     * @param timer 超时时间(秒)
     * @see MessageUtil.nextMessage
     */
    suspend fun Player.nextMessage(group: Group, timer: Int): MessageChain? =
        nextInGroupMessage(this, group, timer)?.message

    /**
     * 获取群组的下一条消息,不区分环境,默认30秒超时
     * 优先使用此方法
     *
     * @see MessageUtil.nextGroupMessage
     */
    suspend fun Group.nextMessage(): MessageChain? = nextGroupMessage(this, 30)

    /**
     * 获取群组的下条消息,不区分环境
     * 优先使用此方法
     *
     * @param timer 超时时间(秒)
     * @see MessageUtil.nextGroupMessage
     */
    suspend fun Group.nextMessage(timer: Int): MessageChain? = nextGroupMessage(this, timer)


    /**
     * 等待指定群组中的下一条消息。
     *
     * @param group 要监听的群组对象
     * @return 收到的消息（[MessageChain]），如果没有收到则挂起直到有消息
     */
    suspend fun nextGroupMessage(group: Group): MessageChain? {
        return nextGroupMessageEvent(group.id)?.message
    }

    /**
     * 等待指定群组中的下一条消息事件。
     *
     * @param group 要监听的群组对象
     * @return 收到的消息事件（[MessageEvent]），如果没有收到则挂起直到有消息
     */
    suspend fun nextGroupMessageEvent(group: Group): GroupMessageEvent? {
        return nextGroupMessageEvent(group.id)
    }

    /**
     * 等待指定群组在一定时间内发送的消息（带超时机制）。
     *
     * @param group 要监听的群组对象
     * @param timer 超时时间（单位：秒）
     * @return 如果在规定时间内收到消息则返回 [MessageChain]，否则返回 null
     */
    suspend fun nextGroupMessage(group: Group, timer: Int): MessageChain? = withTimeoutOrNull(timer * 1000L) {
        nextGroupMessageEvent(group.id)?.message
    }


    /**
     * 等待指定群组在一定时间内发送的消息事件（带超时机制）。
     *
     * @param group 要监听的群组对象
     * @param timer 超时时间（单位：秒）
     * @return 如果在规定时间内收到消息则返回 [MessageEvent]，否则返回 null
     */
    suspend fun nextGroupMessageEvent(group: Group, timer: Int): GroupMessageEvent? = withTimeoutOrNull(timer * 1000L) {
        nextGroupMessageEvent(group.id)
    }

    /**
     * 等待指定群组 ID 的下一条消息事件。
     *
     * @param groupId 要监听的群组 ID
     * @return 收到的消息事件（[MessageEvent]），如果没有收到则挂起直到有消息
     */
    suspend fun nextGroupMessageEvent(groupId: Long): GroupMessageEvent? = callbackFlow {
        val once = TeaFoxGames.channel.filter { it.subject.id == groupId }
            .subscribeOnce<GroupMessageEvent> { trySend(it) }
        // 当 callbackFlow 结束时取消监听器
        awaitClose {
            once.complete()
        }
    }.firstOrNull()


    /**
     * 等待指定玩家发送的下一条消息,不区分环境。
     *
     * @param sender 要监听的玩家对象
     * @return 收到的消息（[MessageChain]），如果没有收到则挂起直到有消息
     */
    @JvmName("nextPlayerMessageFun")
    suspend fun nextMessage(sender: Player): MessageChain? {
        return nextMessage(sender.id)?.message
    }

    /**
     * 等待指定玩家发送的下一条消息事件,不区分环境。
     *
     * @param sender 要监听的玩家对象
     * @return 收到的消息事件（[MessageEvent]），如果没有收到则挂起直到有消息
     */
    suspend fun nextMessageEvent(sender: Player): MessageEvent? {
        return nextMessage(sender.id)
    }

    /**
     * 等待指定玩家在一定时间内发送的消息（带超时机制）,不区分环境。
     *
     * 你也可以直接使用 `player.nextMessage()`
     *
     * @param sender 要监听的玩家对象
     * @param timer 超时时间（单位：秒）
     * @return 如果在规定时间内收到消息则返回 [MessageChain]，否则返回 null
     */
    suspend fun nextMessage(sender: Player, timer: Int): MessageChain? = withTimeoutOrNull(timer * 1000L) {
        nextMessage(sender.id)?.message
    }

    /**
     * 等待指定玩家在一定时间内发送的消息（带超时机制）,不区分环境。
     *
     * 你也可以直接使用 `player.nextMessage()`
     *
     * @param sender 要监听的玩家对象
     * @param timer 超时时间（单位：秒）
     * @return 如果在规定时间内收到消息则返回 [MessageEvent]，否则返回 null
     */
    suspend fun nextMessageEvent(sender: Player, timer: Int): MessageEvent? = withTimeoutOrNull(timer * 1000L) {
        nextMessage(sender.id)
    }

    /**
     * 等待指定用户 ID 发送的下一条消息。
     *
     * @param senderId 要监听的用户 ID
     * @return 收到的消息事件（[MessageEvent]），如果没有收到则挂起直到有消息
     */
    suspend fun nextMessage(senderId: Long): MessageEvent? = callbackFlow {
        val once = TeaFoxGames.channel.filter { it.sender.id == senderId }
            .subscribeOnce<MessageEvent> { trySend(it) }
        awaitClose {
            once.complete()
        }
    }.firstOrNull()


    /**
     * 等待指定群组中该玩家发送的下一条消息。
     *
     * @param player 要监听的玩家对象
     * @param group 要监听的群组对象
     * @return 收到的消息事件（[MessageEvent]），如果没有收到则挂起直到有消息
     */
    suspend fun nextInGroupMessage(player: Player, group: Group): MessageEvent? = nextInGroupMessage(player, group.id)

    /**
     * 等待指定群组中该玩家在一定时间内发送的消息（带超时机制）。
     *
     * @param player 要监听的玩家对象
     * @param group 要监听的群组对象
     * @param timer 超时时间（单位：秒）
     * @return 如果在规定时间内收到消息则返回 [MessageEvent]，否则返回 null
     */
    suspend fun nextInGroupMessage(player: Player, group: Group, timer: Int): MessageEvent? =
        withTimeoutOrNull(timer * 1000L) {
            nextInGroupMessage(player, group.id)
        }

    /**
     * 获取玩家在这个群的下一条消息事件
     *
     */
    suspend fun nextInGroupMessage(player: Player, groupId: Long): MessageEvent? {
        return callbackFlow {
            val once =
                TeaFoxGames.channel.filter { it.subject.id == groupId && it.sender.id == player.id }
                    .subscribeOnce<GroupMessageEvent> { trySend(it) }
            // 当 callbackFlow 结束时取消监听器
            awaitClose {
                once.complete()
            }
        }.firstOrNull()
    }

    //== 发送消息的方法工具

    /**
     * 发送消息给玩家
     */
    suspend fun Player.sendMessage(msg: String) {
        bot.getFriend(this.id)?.sendMessage(msg)
    }

    /**
     * 发送消息给玩家
     */
    suspend fun Player.sendMessage(msg: MessageChain) {
        bot.getFriend(this.id)?.sendMessage(msg)
    }


    // == 其他消息工具 ==
}
