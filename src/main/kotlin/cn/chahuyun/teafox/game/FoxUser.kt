package cn.chahuyun.teafox.game

import cn.chahuyun.teafox.game.data.FoxCoins
import cn.chahuyun.teafox.game.data.FoxUser
import cn.chahuyun.hibernateplus.HibernateFactory
import net.mamoe.mirai.contact.User
import java.time.LocalDateTime


/**
 * FoxUserManager
 *  玩家管理
 * @author Moyu
 * @date 2022/7/17
 */
object FoxUserManager {

    /**
     * 获取用户
     * @param sender 发送者
     * @return FoxUser fox用户
     */
    fun getFoxUser(sender: User): FoxUser = getFoxUser(sender.id) ?: run {
        val user = FoxUser().apply {
            uid = sender.id
            name = sender.nick
            coins = 0
            victory = 0
            lose = 0
        }

        HibernateFactory.merge(user)
    }

    /**
     * 获取用户
     * @param player 玩家
     * @return FoxUser fox用户
     */
    fun getFoxUser(player: Player): FoxUser = getFoxUser(player.id) ?: run {
        val user = FoxUser().apply {
            uid = player.id
            name = player.name
            coins = 0
            victory = 0
            lose = 0
        }

        HibernateFactory.merge(user)
    }

    /**
     * 获取用户
     * @param id id
     * @return FoxUser fox用户
     */
    fun getFoxUser(id: Long): FoxUser? = HibernateFactory.selectOne(FoxUser::class.java, "uid", id)


    /**
     * 添加胜利,同时添加分数
     * @param score 分数
     * @param isLandlord 是否是地主
     */
    fun FoxUser.addVictory(score: Int, isLandlord: Boolean = false) {
        // 增加总金币和胜利次数
        this.coins = (this.coins ?: 0) + score
        this.victory = (this.victory ?: 0) + 1

        // 如果是地主，才增加地主胜利次数
        if (isLandlord) {
            this.landlordVictory = (this.landlordVictory ?: 0) + 1
        }

        HibernateFactory.merge(this)
    }

    /**
     * 添加失败,同时减少分数
     * @param score 分数
     * @param isLandlord 是否是地主
     */
    fun FoxUser.addLose(score: Int, isLandlord: Boolean = false) {
        // 减少金币
        this.coins = (this.coins ?: 0) - score
        // 增加总失败次数
        this.lose = (this.lose ?: 0) + 1

        // 如果是地主，才增加地主失败次数
        if (isLandlord) {
            this.landlordLose = (this.landlordLose ?: 0) + 1
        }

        HibernateFactory.merge(this)
    }

}

/**
 * 玩家金币管理
 *
 * @author Moyu
 */
object FoxCoinsManager {

    /**
     * 领取金币，每天只能领3次
     * @return true 领取成功
     */
    fun receiveGoldCoins(foxUser: FoxUser): Pair<Boolean, Int> {
        val now = LocalDateTime.now()

        val coins = foxUser.foxCoins ?: run {
            // 如果不存在记录，创建新记录，初始次数为1，时间为当前
            val newCoins = FoxCoins().apply {
                time = now
                timer = 1
            }
            foxUser.foxCoins = HibernateFactory.merge(newCoins)
            addScoreToUser(foxUser)
            return Pair(true, 1)
        }

        // 判断是否是同一天
        val isSameDay = isSameDay(coins.time, now)

        if (isSameDay) {
            // 同一天，判断领取次数
            val currentTimer = coins.timer ?: 1
            if (currentTimer >= 3) {
                // 已达上限，返回失败
                return Pair(false, 3)
            } else {
                // 次数+1
                coins.timer = currentTimer + 1
                HibernateFactory.merge(coins)

                addScoreToUser(foxUser)
                return Pair(true, coins.timer!!)
            }
        } else {
            // 不是同一天，重置次数为1，更新时间为当前时间
            coins.time = now
            coins.timer = 1
            HibernateFactory.merge(coins)

            addScoreToUser(foxUser)
            return Pair(true, 1)
        }
    }

    // 辅助函数：判断两个日期是否是同一天
    private fun isSameDay(date1: LocalDateTime?, date2: LocalDateTime?): Boolean {
        if (date1 == null || date2 == null) return false
        return date1.toLocalDate() == date2.toLocalDate()
    }

    // 辅助函数：添加积分到用户
    private fun addScoreToUser(foxUser: FoxUser, amount: Int = 1000) {
        foxUser.coins = foxUser.coins?.plus(amount) ?: amount
        HibernateFactory.merge(foxUser)
    }
}