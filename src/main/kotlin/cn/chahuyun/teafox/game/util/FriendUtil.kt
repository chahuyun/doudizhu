package cn.chahuyun.teafox.game.util

import cn.chahuyun.teafox.game.TeaFoxGames
import cn.chahuyun.teafox.game.Player
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import net.mamoe.mirai.event.ListeningStatus
import net.mamoe.mirai.event.events.NewFriendRequestEvent

/**
 * 好友工具
 */
class FriendUtil(private val players:List<Player>) {

    /**
     * 非好友玩家
     */
    private  var unFriend = mutableListOf<Player>()

    /**
     * 检查所有玩家是否已经是好友，如果是则返回true。
     */
    fun check(): Boolean {
        unFriend.clear() // 清空未添加好友列表
        players.forEach {
            if (TeaFoxGames.bot.getFriend(it.id) == null) {
                unFriend.add(it)
            }
        }
        return unFriend.isEmpty()
    }

    /**
     * 监听新好友请求事件，直到所有玩家都成为好友或超时。
     */
    suspend fun listening(): Boolean {
        return callbackFlow {
            val subscribe = TeaFoxGames.scope.subscribe<NewFriendRequestEvent> { event ->
                val player = unFriend.find { it.id == event.fromId }
                if (player != null) {
                    unFriend.remove(player)
                    event.accept()
                    if (unFriend.isEmpty()) {
                        trySend(true)
                        ListeningStatus.STOPPED
                    } else {
                        ListeningStatus.LISTENING
                    }
                } else {
                    event.reject(false)
                    ListeningStatus.LISTENING
                }
            }
            awaitClose{
                subscribe.complete()
            }
        }.firstOrNull() ?: false
    }

}


