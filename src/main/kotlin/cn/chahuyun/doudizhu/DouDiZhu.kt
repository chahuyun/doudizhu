package cn.chahuyun.doudizhu

import cn.chahuyun.authorize.PermissionServer
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.BotOnlineEvent
import net.mamoe.mirai.event.events.MessageEvent

object DouDiZhu : KotlinPlugin(
    JvmPluginDescription(
        "cn.chahuyun.doudizhu", "1.0.0", "斗地主"
    ) {
        author("moyuyanli")
        info("FeaFox-DouDiZhu")
    }
) {
    /**
     * 全局bot,默认第一个登录的bot
     */
    lateinit var bot: Bot

    /**
     * 全局事件通道
     */
    lateinit var scope: EventChannel<Event>

    /**
     * 全局息事件通道
     */
    lateinit var channel: EventChannel<MessageEvent>
    override fun onEnable() {
        DZConfig.reload()

        PermissionServer.registerMessageEvent(this, "cn.chahuyun.doudizhu")


        val scope = GlobalEventChannel.parentScope(this)
        this@DouDiZhu.scope = scope
        channel = scope.filterIsInstance(MessageEvent::class)
        scope.filterIsInstance(BotOnlineEvent::class).subscribeOnce<BotOnlineEvent> {
            this@DouDiZhu.bot = it.bot
            logger.info("斗地主bot已配置为${it.bot.nameCardOrNick}")
        }
    }


    override fun onDisable() {
    }
}