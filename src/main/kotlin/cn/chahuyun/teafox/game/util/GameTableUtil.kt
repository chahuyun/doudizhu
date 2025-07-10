package cn.chahuyun.teafox.game.util

import cn.chahuyun.teafox.game.Player
import cn.chahuyun.teafox.game.game.GameTable
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.PlainText


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
}