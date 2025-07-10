package cn.chahuyun.teafox.game

import cn.chahuyun.authorize.PermissionServer
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.extension.PluginComponentStorage
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.BotOnlineEvent
import net.mamoe.mirai.event.events.MessageEvent

object TeaFoxGames : KotlinPlugin(
    JvmPluginDescription(
        "cn.chahuyun.teafox.games", "1.0.0", "TeaFoxGames"
    ) {
        author("moyuyanli")
        info("TeaFoxGames 茶狐云的游戏机")

        dependsOn("cn.chahuyun.HuYanAuthorize", ">=1.2.0", false)
//        dependsOn("cn.chahuyun.HuYanEconomy",">=1.7.6",true)
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

    override fun PluginComponentStorage.onLoad() {
        logger.debug("TeaFoxGames 开始加载!")
    }

    override fun onEnable() {
        logger.debug("TeaFoxGames 启动中...")
        DZConfig.reload()
        DZDataConfig.reload()

        DataManager.init()

        PermissionServer.registerMessageEvent(this, "cn.chahuyun.teafox.game")

        val scope = GlobalEventChannel.parentScope(this)
        this@TeaFoxGames.scope = scope
        channel = scope.filterIsInstance(MessageEvent::class)
        scope.filterIsInstance(BotOnlineEvent::class).subscribeOnce<BotOnlineEvent> {
            this@TeaFoxGames.bot = it.bot
            logger.info("游戏机bot已配置为${it.bot.nameCardOrNick}")
        }
    }


    override fun onDisable() {
        logger.debug("TeaFoxGames 已卸载...")
    }

    fun info(msg: String) {
        logger.info(msg)
    }

    fun warning(msg: String) {
        logger.warning(msg)
    }

    fun debug(msg: String) {
        logger.debug(msg)
    }

    fun error(msg: String) {
        logger.error(msg)
    }

    fun error(msg: String?, e: Throwable?) {
        logger.error(msg, e)
    }

    fun error(e: Throwable?) {
        logger.error(e)
    }
}
