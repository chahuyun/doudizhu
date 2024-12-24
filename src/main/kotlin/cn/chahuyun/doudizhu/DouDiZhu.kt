package cn.chahuyun.doudizhu

import cn.chahuyun.authorize.PermissionServer
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent

object DouDiZhu : KotlinPlugin(
    JvmPluginDescription(
        "cn.chahuyun.doudizhu", "1.0.0", "斗地主"
    ) {
        author("moyuyanli")
        info("FeaFox-DouDiZhu")
    }
) {

    lateinit var channel: EventChannel<MessageEvent>
    override fun onEnable() {
        DZConfig.reload()

        PermissionServer.registerMessageEvent(this, "cn.chahuyun.doudizhu")

        val scope = GlobalEventChannel.parentScope(this)
        channel = scope.filterIsInstance(MessageEvent::class)

    }


    override fun onDisable() {
    }
}