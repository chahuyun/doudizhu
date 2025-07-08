package cn.chahuyun.doudizhu.util

import cn.chahuyun.doudizhu.DouDiZhu
import cn.chahuyun.doudizhu.Player
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent

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
@Suppress("MemberVisibilityCanBePrivate")
object MessageUtil {

    /**
     * 等待指定群组中的下一条消息。
     *
     * @param group 要监听的群组对象
     * @return 收到的消息事件（[MessageEvent]），如果没有收到则挂起直到有消息
     */
    suspend fun nextGroupMessage(group: Group): MessageEvent? {
        return nextGroupMessage(group.id)
    }

    /**
     * 等待指定群组在一定时间内发送的消息（带超时机制）。
     *
     * @param group 要监听的群组对象
     * @param timer 超时时间（单位：秒）
     * @return 如果在规定时间内收到消息则返回 [MessageEvent]，否则返回 null
     */
    suspend fun nextGroupMessage(group: Group, timer: Int): MessageEvent? = withTimeoutOrNull(timer * 1000L) {
        nextGroupMessage(group.id)
    }

    /**
     * 等待指定群组 ID 的下一条消息。
     *
     * @param groupId 要监听的群组 ID
     * @return 收到的消息事件（[MessageEvent]），如果没有收到则挂起直到有消息
     */
    suspend fun nextGroupMessage(groupId: Long): MessageEvent? = callbackFlow {
        val once = DouDiZhu.channel.filter { it.subject.id == groupId }
            .subscribeOnce<GroupMessageEvent> { trySend(it) }
        // 当 callbackFlow 结束时取消监听器
        awaitClose {
            once.complete()
        }
    }.firstOrNull()

    /**
     * 等待指定玩家发送的下一条私聊消息。
     *
     * @param sender 要监听的玩家对象
     * @return 收到的消息事件（[MessageEvent]），如果没有收到则挂起直到有消息
     */
    suspend fun nextMessage(sender: Player): MessageEvent? {
        return nextMessage(sender.id)
    }

    /**
     * 等待指定玩家在一定时间内发送的消息（带超时机制）。
     *
     * @param sender 要监听的玩家对象
     * @param timer 超时时间（单位：秒）
     * @return 如果在规定时间内收到消息则返回 [MessageEvent]，否则返回 null
     */
    suspend fun nextMessage(sender: Player, timer: Int): MessageEvent? = withTimeoutOrNull(timer * 1000L) {
        nextMessage(sender.id)
    }

    /**
     * 等待指定用户 ID 发送的下一条消息。
     *
     * @param senderId 要监听的用户 ID
     * @return 收到的消息事件（[MessageEvent]），如果没有收到则挂起直到有消息
     */
    suspend fun nextMessage(senderId: Long): MessageEvent? = callbackFlow {
        val once = DouDiZhu.channel.filter { it.sender.id == senderId }
            .subscribeOnce<MessageEvent> { trySend(it) }
        awaitClose {
            once.complete()
        }
    }.firstOrNull()
}