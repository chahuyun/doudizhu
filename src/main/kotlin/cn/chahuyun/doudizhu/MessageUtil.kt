package cn.chahuyun.doudizhu

import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.event.events.MessageEvent

object MessageUtil {


    suspend fun nextMessage(sender: Contact): MessageEvent {
        return nextMessage(sender.id)
    }

    suspend fun nextMessage(senderId: Long): MessageEvent = callbackFlow {
        DouDiZhu.channel.filter { it.sender.id == senderId }.subscribeOnce<MessageEvent> { trySend(it) }
    }.first()


}