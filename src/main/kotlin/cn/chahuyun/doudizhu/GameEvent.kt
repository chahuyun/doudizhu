package cn.chahuyun.doudizhu

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.authorize.utils.MessageUtil.sendMessageQuery
import cn.chahuyun.doudizhu.DouDiZhu.debug
import cn.chahuyun.doudizhu.FoxUserManager.getFoxUser
import cn.chahuyun.doudizhu.data.FoxUser
import cn.chahuyun.doudizhu.game.GameTable
import cn.chahuyun.doudizhu.util.CustomForwardDisplayStrategy
import cn.chahuyun.doudizhu.util.MessageUtil.nextGroupMessage
import cn.chahuyun.hibernateplus.HibernateFactory
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.buildForwardMessage

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
    suspend fun startGameMin(event: GroupMessageEvent) = startGame(event, GameTableCoinsType.NORMAL)

    @MessageAuthorize(text = ["开桌 大", "┳━┳", "来一局大的"])
    suspend fun startGameMax(event: GroupMessageEvent) = startGame(event, GameTableCoinsType.BIG)

    @MessageAuthorize(text = ["开桌 绝杀", "整一局绝杀局", "来父子局"])
    suspend fun startGameHuge(event: GroupMessageEvent) = startGame(event, GameTableCoinsType.HUGE)

    @MessageAuthorize(text = ["开桌 巅峰", "来巅峰局","来癫疯局"])
    suspend fun startGamePeak(event: GroupMessageEvent) = startGame(event, GameTableCoinsType.PEAK)

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
            displayStrategy =
            CustomForwardDisplayStrategy(
                titleGenerator = "群聊的聊天记录",
                previewGenerator = listOf(
                    "${DZConfig.botName}:分享一个炸裂的瓜!",
                    "${sender.nameCardOrNick}:卧槽,这是真的吗?",
                    "${DZConfig.botName}:[图片]"
                ),
                summarySize = 10
            )
        ) {
            bot named DZConfig.botName says "以下是胜率排行榜↓:"
            list.forEach {
                it.uid!! named it.name!! says """
                    No.${no++}
                    用户名:${it.name}
                    狐币:${it.coins}
                    胜率:${it.winRate()}%
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
            displayStrategy =
            CustomForwardDisplayStrategy(
                titleGenerator = "群聊的聊天记录",
                previewGenerator = listOf(
                    "${sender.nameCardOrNick}:给兄弟们来点豪堪的!",
                    "${sender.nameCardOrNick}:[图片]",
                    "${sender.nameCardOrNick}:[图片]",
                ),
                summarySize = 10
            )
        ) {
            bot named DZConfig.botName says "以下是狐币排行榜↓:"
            list.forEach {
                it.uid!! named it.name!! says """
                    No.${no++}
                    用户名:${it.name}
                    狐币:${it.coins}
                    胜率:${it.winRate()}%
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
    private suspend fun startGame(event: GroupMessageEvent, type: GameTableCoinsType) {
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
            gameTables[group.id] = GameTable(listOf(), event.bot, group)
        }

        val action = when (type) {
            GameTableCoinsType.NORMAL -> "普通"
            GameTableCoinsType.BIG -> "豪华"
            GameTableCoinsType.HUGE -> "绝杀"
            else -> "巅峰"
        }

        group.sendMessage("${sender.nick} 开启了一桌 $action 对局游戏，快快发送 加入对局|来 加入对局进行游戏吧!")

        // 使用可变列表来添加玩家
        val players = mutableListOf(Player(sender.id, sender.nameCardOrNick))

        debug("等待加入游戏!")
        while (players.size < 3) {
            val messageEvent = nextGroupMessage(group, DZConfig.timeOut) ?: run {
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
                if (!players.any { it.id == newPlayer.id }) { // 防止重复加入
                    players.add(newPlayer)
                    group.sendMessage("${newPlayer.name} 加入了游戏！当前玩家：${players.joinToString(",") { it.name }}")
                }
            }else if (content.matches("^掀桌".toRegex())) {
                group.sendMessage("掀桌(╯‵□′)╯︵┻━┻")
                gameTables.remove(group.id)
                return
            }
        }

        val gameTable = GameTable(players, event.bot, group, type = type)
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
}
