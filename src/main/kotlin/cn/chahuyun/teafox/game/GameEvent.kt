package cn.chahuyun.teafox.game

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.utils.MessageUtil.sendMessageQuery
import cn.chahuyun.hibernateplus.HibernateFactory
import cn.chahuyun.teafox.game.FoxUserManager.getFoxUser
import cn.chahuyun.teafox.game.TeaFoxGames.debug
import cn.chahuyun.teafox.game.data.FoxUser
import cn.chahuyun.teafox.game.game.DizhuGameTable
import cn.chahuyun.teafox.game.game.GameTable
import cn.chahuyun.teafox.game.game.GandGameTable
import cn.chahuyun.teafox.game.util.MessageUtil.buildForwardMessage
import cn.chahuyun.teafox.game.util.MessageUtil.nextGroupMessageEvent
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.PlainText

/**
 * 游戏事件
 *
 * @author Moyuyanli
 * @date 2024-12-13 11:09
 */
@Suppress("DuplicatedCode", "unused")
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


    @MessageAuthorize(text = ["开桌", "来一局"])
    suspend fun startGameMin(event: GroupMessageEvent) = startDoudizhuGame(event, GameTableCoinsType.NORMAL)

    @MessageAuthorize(text = ["开桌 大", "┳━┳", "来局大的", "来一局大的"])
    suspend fun startGameMax(event: GroupMessageEvent) = startDoudizhuGame(event, GameTableCoinsType.BIG)

    @MessageAuthorize(text = ["开桌 绝杀", "整一局绝杀局", "来父子局"])
    suspend fun startGameHuge(event: GroupMessageEvent) = startDoudizhuGame(event, GameTableCoinsType.HUGE)

    @MessageAuthorize(text = ["开桌 巅峰", "来巅峰局", "来癫疯局"])
    suspend fun startGamePeak(event: GroupMessageEvent) = startDoudizhuGame(event, GameTableCoinsType.PEAK)

    @MessageAuthorize(text = ["开桌 干瞪眼", "来局干瞪眼"])
    suspend fun startGGame(event: GroupMessageEvent) = startGandGame(event)

    @MessageAuthorize(text = ["我的狐币", "狐币"])
    suspend fun viewFoxCoins(event: GroupMessageEvent) {
        val foxUser = getFoxUser(event.sender)
        event.sendMessageQuery("当前狐币：${foxUser.coins}!")
    }

    @MessageAuthorize(text = ["胜率榜"])
    suspend fun viewVictory(event: GroupMessageEvent) {
        val list = HibernateFactory.selectListByHql(
            FoxUser::class.java,
            "FROM FoxUser ORDER BY (victory / (victory + lose + 0.0)) DESC limit 10",
            mutableMapOf()
        ).filter { (it.victory ?: 0) + (it.lose ?: 0) >= 5 }
        val bot = event.bot
        val sender = event.sender

        var no = 1
        val message = event.buildForwardMessage(
            titleGenerator = "群聊的聊天记录", previewGenerator = listOf(
                "${DZConfig.botName}:分享一个炸裂的瓜!",
                "${DZConfig.botName}:[图片]",
                "${sender.nameCardOrNick}:卧槽,这是真的吗?"
            ), summarySize = 11
        ) {
            bot named DZConfig.botName says "以下是胜率排行榜↓:"
            list.forEach {
                it.uid!! named it.name!! says """
                    No.${no++}
                    用户名:${it.name}
                    狐币:${it.coins}
                    积分:${it.integral()}
                    胜率:${it.winRate()}
                    地主胜率:${it.landlordWinRate()}
                    农民胜率:${it.farmerWinRate()}
                    总场次:${it.victory!! + it.lose!!}
                """.trimIndent()
            }
        }

        event.subject.sendMessage(message)
    }

    @MessageAuthorize(text = ["狐币榜"])
    suspend fun viewCoins(event: GroupMessageEvent) {
        val list = HibernateFactory.selectListByHql(
            FoxUser::class.java,
            "FROM FoxUser ORDER BY coins DESC limit 10",
            mutableMapOf()
        ).filter { (it.victory ?: 0) + (it.lose ?: 0) >= 5 }
        val bot = event.bot
        val sender = event.sender

        var no = 1
        val message = event.buildForwardMessage(
            titleGenerator = "群聊的聊天记录", previewGenerator = listOf(
                "${sender.nameCardOrNick}:给兄弟们来点豪堪的!",
                "${sender.nameCardOrNick}:[图片]",
                "${sender.nameCardOrNick}:[图片]",
            ), summarySize = 11
        ) {
            bot named DZConfig.botName says "以下是狐币排行榜↓:"
            list.forEach {
                it.uid!! named it.name!! says """
                    No.${no++}
                    用户名:${it.name}
                    狐币:${it.coins}
                    积分:${it.integral()}
                    胜率:${it.winRate()}
                    地主胜率:${it.landlordWinRate()}
                    农民胜率:${it.farmerWinRate()}
                    总场次:${it.victory!! + it.lose!!}
                """.trimIndent()
            }
        }

        event.subject.sendMessage(message)
    }

    @MessageAuthorize(text = ["积分榜"])
    suspend fun viewIntegral(event: GroupMessageEvent) {
        val list = HibernateFactory.selectListByHql(
            FoxUser::class.java,
            "FROM FoxUser ORDER BY (landlordVictory - landlordLose) + (victory - lose) DESC limit 10",
            mutableMapOf()
        ).filter { (it.victory ?: 0) + (it.lose ?: 0) >= 5 }
        val bot = event.bot
        val sender = event.sender

        var no = 1
        val message = event.buildForwardMessage(
            titleGenerator = "群聊的聊天记录", previewGenerator = listOf(
                "${sender.nameCardOrNick}:[图片]",
                "${sender.nameCardOrNick}:[图片]",
                "${DZConfig.botName}:你简直是甜菜!!!",
            ), summarySize = 11
        ) {
            bot named DZConfig.botName says "以下是积分排行榜↓:"
            list.forEach {
                it.uid!! named it.name!! says """
                    No.${no++}
                    用户名:${it.name}
                    狐币:${it.coins}
                    积分:${it.integral()}
                    胜率:${it.winRate()}
                    地主胜率:${it.landlordWinRate()}
                    农民胜率:${it.farmerWinRate()}
                    总场次:${it.victory!! + it.lose!!}
                """.trimIndent()
            }
        }

        event.subject.sendMessage(message)
    }

    //=辅助私有方法

    /**
     * 开始游戏
     */
    private suspend fun startDoudizhuGame(event: GroupMessageEvent, type: GameTableCoinsType) {
        val sender = event.sender
        val group = event.group

        val foxUser = getFoxUser(sender)

        if (foxUser.coins!! < type.guaranteed) {
            if (!getFoxCoins(foxUser, event, type)) return
        }

        if (gameTables.containsKey(group.id)) {
            group.sendMessage("游戏桌 ┳━┳ 已存在，请勿重复创建")
            return
        } else {
            gameTables[group.id] = DizhuGameTable(group, listOf(), event.bot)
        }
        val player = Player(sender.id, sender.nameCardOrNick)

        if (!checkPlayerInGame(player, group)) {
            group.sendMessage("${player.name} 你已加入其他游戏桌，请勿重复加入")
        }

        val action = when (type) {
            GameTableCoinsType.NORMAL -> "普通"
            GameTableCoinsType.BIG -> "豪华"
            GameTableCoinsType.HUGE -> "绝杀"
            else -> "巅峰"
        }

        group.sendMessage("${player.name} 开启了一桌 $action 对局游戏，快快发送 加入对局|来 加入对局进行游戏吧!")

        // 使用可变列表来添加玩家
        val players = mutableListOf(player)

        debug("等待加入游戏!")
        while (players.size < 3) {
            val messageEvent = nextGroupMessageEvent(group, DZConfig.timeOut) ?: run {
                group.sendMessage("等待玩家加入超时，游戏未能开始(╯‵□′)╯︵┻━┻")
                gameTables.remove(group.id)
                return // 如果超时则退出
            }

            val content = messageEvent.message.contentToString()
            if (content.matches("^加入|来".toRegex())) { // 检查消息内容是否为加入请求
                val newPlayer = Player(messageEvent.sender.id, messageEvent.sender.nick)
                val playerFoxUser = getFoxUser(newPlayer)

                if (playerFoxUser.coins!! < type.guaranteed) {
                    if (!getFoxCoins(playerFoxUser, messageEvent, type)) continue
                }
                // 防止重复加入
                if (players.any { it.id == newPlayer.id } || !checkPlayerInGame(newPlayer, group)) continue

                players.add(newPlayer)
                group.sendMessage("${newPlayer.name} 加入了游戏！当前玩家：${players.joinToString(",") { it.name }}")
            } else if (content.matches("^掀桌".toRegex())) {
                group.sendMessage("掀桌(╯‵□′)╯︵┻━┻")
                gameTables.remove(group.id)
                return
            }
        }

        val gameTable = DizhuGameTable(group, players, event.bot, type = type)
        gameTables[group.id] = gameTable
        gameTable.start()
    }

    private suspend fun startGandGame(event: GroupMessageEvent) {
        val sender = event.sender
        val group = event.group

        val type = GameTableCoinsType.HUGE
        val foxUser = getFoxUser(sender)

        if (foxUser.coins!! < type.guaranteed) {
            if (!getFoxCoins(foxUser, event, type)) return
        }

        if (gameTables.containsKey(group.id)) {
            group.sendMessage("游戏桌 ┳━┳ 已存在，请勿重复创建")
            return
        } else {
            gameTables[group.id] = GandGameTable(group, listOf(), GameType.GAND)
        }
        val player = Player(sender.id, sender.nameCardOrNick)

        if (!checkPlayerInGame(player, group)) {
            group.sendMessage("${player.name} 你已加入其他游戏桌，请勿重复加入")
        }

        group.sendMessage("${player.name} 开启了一桌 干瞪眼 对局游戏，快快发送 加入对局|来 加入对局进行游戏吧!")

        // 使用可变列表来添加玩家
        val players = mutableListOf(player)

        debug("等待加入游戏!")
        while (players.size < 6) {
            val messageEvent = nextGroupMessageEvent(group, DZConfig.timeOut) ?: run {
                group.sendMessage("等待玩家加入超时，游戏未能开始(╯‵□′)╯︵┻━┻")
                gameTables.remove(group.id)
                return // 如果超时则退出
            }

            val content = messageEvent.message.contentToString()
            if (content.matches("^加入|来".toRegex())) { // 检查消息内容是否为加入请求
                val newPlayer = Player(messageEvent.sender.id, messageEvent.sender.nick)
                val playerFoxUser = getFoxUser(newPlayer)

                if (playerFoxUser.coins!! < type.guaranteed) {
                    if (!getFoxCoins(playerFoxUser, messageEvent, type)) continue
                }
                // 防止重复加入
                if (players.any { it.id == newPlayer.id } || !checkPlayerInGame(newPlayer, group)) continue

                players.add(newPlayer)
                group.sendMessage("${newPlayer.name} 加入了游戏！当前玩家：${players.joinToString(",") { it.name }}")
            } else if (content.matches("^开始|开".toRegex())) {
                group.sendMessage("当前玩家：${players.joinToString(",") { it.name }} 开始干瞪眼游戏!")
                break
            } else if (content.matches("^掀桌".toRegex())) {
                group.sendMessage("掀桌(╯‵□′)╯︵┻━┻")
                gameTables.remove(group.id)
                return
            }
        }

        val gameTable = GandGameTable(group, players, GameType.GAND)
        gameTables[group.id] = gameTable
        gameTable.start()
    }

    /**
     * 自动化领取免费狐币!
     * @return true 大于 [GameTableCoinsType.guaranteed]保底值 可以游戏
     */
    private suspend fun getFoxCoins(foxUser: FoxUser, event: GroupMessageEvent, type: GameTableCoinsType): Boolean {
        if (foxUser.coins == null) return false
        var reply: MessageChain = At(event.sender).plus(PlainText("尝试领取今日免费狐币:\n"))
        while (foxUser.coins!! < type.guaranteed) {
            val coins = FoxCoinsManager.receiveGoldCoins(foxUser)
            if (coins.first) {
                reply = reply.plus(PlainText("今日第${coins.second}次,当前狐币：${foxUser.coins}!\n"))
            } else {
                reply = reply.plus(PlainText("免费次数已用完,你的狐币仍然不够 ${type.guaranteed} !"))
                event.subject.sendMessage(reply)
                return false
            }
        }
        event.subject.sendMessage(reply)
        return true
    }

    /**
     * 检查玩家是否在其他游戏桌里面
     * @return true 不在
     */
    private fun checkPlayerInGame(player: Player, group: Group): Boolean {
        return if (gameTables.size == 1 && gameTables.containsKey(group.id)) {
            true
        } else {
            !gameTables.filter { it.key != group.id }.any { player in it.value.players }
        }
    }
}
