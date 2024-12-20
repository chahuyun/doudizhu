package cn.chahuyun.doudizhu

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object DZConfig : AutoSavePluginConfig("doudizhu-config") {


    @ValueDescription("bot的代称")
    val botName: String by value("bot")


}