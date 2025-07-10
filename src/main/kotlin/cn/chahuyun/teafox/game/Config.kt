package cn.chahuyun.teafox.game

import cn.chahuyun.hibernateplus.DriveType
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object DZConfig : AutoSavePluginConfig("doudizhu-config") {


    @ValueDescription("bot的代称")
    val botName: String by value("bot")

    @ValueDescription("创建对局等待时间")
    val timeOut: Int by value(60)

}

object DZDataConfig: AutoSavePluginConfig("doudizhu-data") {

    @ValueDescription("数据库类型(H2,MYSQL,SQLITE)")
    var dataType: DriveType by value(DriveType.H2)

    @ValueDescription("数据库连接地址")
    var mysqlUrl : String by value("localhost:3306/test")

    @ValueDescription("mysql 用户名")
    var mysqlUser: String by value("root")

    @ValueDescription("mysql 密码")
    var mysqlPassword: String by value("123456")
}