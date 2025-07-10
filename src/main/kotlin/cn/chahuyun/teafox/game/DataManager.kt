package cn.chahuyun.teafox.game

import cn.chahuyun.teafox.game.TeaFoxGames.debug
import cn.chahuyun.hibernateplus.DriveType
import cn.chahuyun.hibernateplus.HibernatePlusService

/**
 * DataManager
 *
 * @author Moyuyanli
 * @date 2023/8/5 16:07
 */
object DataManager {

    fun init() {
        val configuration = HibernatePlusService.createConfiguration(TeaFoxGames::class.java)

        configuration.packageName = "cn.chahuyun.teafox.game.data"
        when (DZDataConfig.dataType) {
            DriveType.H2 -> {
                configuration.driveType = DriveType.H2
                configuration.address = TeaFoxGames.dataFolderPath.resolve("FoxDoudizhu.h2").toString()
            }

            DriveType.SQLITE -> {
                configuration.driveType = DriveType.SQLITE
                configuration.address = TeaFoxGames.dataFolderPath.resolve("FoxDoudizhu").toString()
            }

            DriveType.MYSQL -> {
                configuration.driveType = DriveType.MYSQL
                configuration.address = DZDataConfig.mysqlUrl
                configuration.user = DZDataConfig.mysqlUser
                configuration.password = DZDataConfig.mysqlPassword
                configuration.isAutoReconnect = true
            }
        }

        HibernatePlusService.loadingService(configuration)
        debug("数据初始化完成")
    }
}