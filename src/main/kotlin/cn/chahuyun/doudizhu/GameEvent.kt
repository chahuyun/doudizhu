package cn.chahuyun.doudizhu

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.doudizhu.DouDiZhu.debug
import cn.chahuyun.doudizhu.game.GameTable
import cn.chahuyun.doudizhu.util.MessageUtil.nextGroupMessage
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.events.GroupMessageEvent

/**
 * 游戏事件
 *
 * @author Moyuyanli
 * @date 2024-12-13 11:09
 */
@EventComponent
class GameEvent {

    companion object {
        /**
         * 游戏桌
         */
        private val gameTables: MutableMap<Long, GameTable> = mutableMapOf()

        fun cancelGame(group: Group) {
            gameTables.remove(group.id)
        }
    }


    @MessageAuthorize(text = ["开桌", "┳━┳", "来一局"])
    suspend fun startGame(event: GroupMessageEvent) {
        val sender = event.sender
        val group = event.group

        if (gameTables.containsKey(group.id)) {
            group.sendMessage("游戏桌 ┳━┳ 已存在，请勿重复创建")
            return
        } else {
            gameTables[group.id] = GameTable(listOf(), event.bot, group)
        }

        group.sendMessage("${sender.nick} 开启了一桌游戏，快快发送 加入对局|┳━┳ 加入对局进行游戏吧!")

        // 使用可变列表来添加玩家
        val players = mutableListOf(Player(sender.id, sender.nameCard))

        debug("while")
        while (players.size < 3) {
            debug("等待加入游戏!")
            val messageEvent = nextGroupMessage(group) ?: run {
                group.sendMessage("等待玩家加入超时，游戏未能开始。")
                return // 如果超时则退出
            }

            if (messageEvent.message.contentToString().matches("^加入|对局|来|┳━┳".toRegex())) { // 检查消息内容是否为加入请求
                val newPlayer = Player(messageEvent.sender.id, messageEvent.sender.nick)
                if (!players.any { it.id == newPlayer.id }) { // 防止重复加入
                    players.add(newPlayer)
                    group.sendMessage("${newPlayer.name} 加入了游戏！当前人数：${players.size}")
                }
            }
        }

        val gameTable = GameTable(players, event.bot, group)
        gameTables[group.id] = gameTable
        gameTable.start()
    }

}
