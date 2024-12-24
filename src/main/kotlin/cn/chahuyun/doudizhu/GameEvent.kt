package cn.chahuyun.doudizhu

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import kotlinx.coroutines.time.withTimeoutOrNull
import kotlinx.coroutines.withTimeoutOrNull
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.content

/**
 * 游戏事件
 *
 * @author Moyuyanli
 * @date 2024-12-13 11:09
 */
@EventComponent
class GameEvent {

    @MessageAuthorize(text = ["开桌"])
    suspend fun startGame(event: GroupMessageEvent) {
        val sender = event.sender
        val group = event.group

        group.sendMessage("${sender.nick} 开启了一桌游戏，快快发送加入对局进行游戏吧!")

        val players = mutableListOf<Player>() // 使用可变列表来添加玩家
        players.add(Player(sender.id, sender.nameCard, mutableListOf())) // 添加发起者为第一个玩家

        while (players.size < 3) {
            val messageEvent = withTimeoutOrNull(60_000) { // 等待60秒
                MessageUtil.nextMessage(sender)
            } ?: run {
                group.sendMessage("等待玩家加入超时，游戏未能开始。")
                return // 如果超时则退出
            }

            if (messageEvent.message.content.startsWith("加入")) { // 检查消息内容是否为加入请求
                val newPlayer = Player(messageEvent.sender.id, messageEvent.sender.nick, mutableListOf())
                if (!players.any { it.id == newPlayer.id }) { // 防止重复加入
                    players.add(newPlayer)
                    group.sendMessage("${newPlayer.name} 加入了游戏！当前人数：${players.size}")
                }
            }
        }

        // 游戏准备就绪，可以开始分发牌等后续逻辑...
    }


}
