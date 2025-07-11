package cn.chahuyun.teafox.game.data

import cn.chahuyun.hibernateplus.HibernateFactory
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 玩家信息
 */
@Entity
@Table(name = "fox_user")
data class FoxUser(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    /**
     * 玩家id qq
     */
    var uid: Long? = null,
    /**
     * 玩家名称
     */
    var name: String? = null,
    /**
     * 玩家狐币
     */
    var coins: Int? = null,
    /**
     * 玩家胜利次数
     */
    var victory: Int? = null,
    /**
     * 玩家失败次数
     */
    var lose: Int? = null,
    /**
     * 地主胜利次数
     */
    var landlordVictory: Int? = null,
    /**
     * 地主失败次数
     */
    var landlordLose: Int? = null,


    /**
     * 玩家 狐币 领取记录
     */
    @OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(
        name = "fox_coins_id",
        foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    var foxCoins: FoxCoins? = null,


    /**
     * 玩家游戏工具
     */
    @OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(
        name = "fox_game_id",
        foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    var foxGame: FoxGameAuxiliary? = null,
) {
    /**
     * 获取积分
     */
    fun integral(): Int {
        val lw = landlordVictory ?: 0
        val ll = landlordLose ?: 0
        val w = victory ?: 0
        val l = lose ?: 0

        return (lw - ll) + (w - l)
    }

    /**
     * 获取胜率，保留一位小数（例如：66.7）
     */
    fun winRate(): Double {
        val total = (victory ?: 0) + (lose ?: 0)
        if (total == 0) return 0.0
        val rate = (victory ?: 0).toDouble() / total.toDouble() * 100
        return String.format("%.1f%%", rate).toDouble()
    }

    /**
     * 获取地主胜率，返回格式如 "66.7%"
     */
    fun landlordWinRate(): String {
        val lw = landlordVictory ?: 0
        val ll = landlordLose ?: 0
        val total = lw + ll
        if (total == 0) return "0.0%"

        val rate = lw.toDouble() / total.toDouble() * 100
        return String.format("%.1f%%", rate)
    }

    /**
     * 获取农民胜率，返回格式如 "66.7%"
     */
    fun farmerWinRate(): String {
        val lw = landlordVictory ?: 0
        val ll = landlordLose ?: 0
        val w = victory ?: 0
        val l = lose ?: 0

        val farmerWin = w - lw
        val farmerLose = l - ll
        val total = farmerWin + farmerLose

        if (total == 0) return "0.0%"

        val rate = farmerWin.toDouble() / total.toDouble() * 100
        return String.format("%.1f%%", rate)
    }

    /**
     * 添加狐币
     */
    fun addCoins(coins: Int) {
        this.coins = (this.coins ?: 0) + coins
        HibernateFactory.merge(this)
    }

    /**
     * 减少狐币
     */
    fun minusCoins(coins: Int) {
        this.coins = (this.coins ?: 0) - coins
        HibernateFactory.merge(this)
    }
}

/**
 * 玩家辅主工具
 */
@Entity
@Table(name = "fox_game")
data class FoxGameAuxiliary(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    /**
     * 记牌器
     */
    var aScorer: Boolean? = null,

    /**
     * 记牌器时效
     */
    var aScorerTime: LocalDateTime? = null,

    /**
     * 超级加倍特权
     */
    var superDouble: Boolean? = null,

    /**
     * 超级加倍特权时效
     */
    var superDoubleTime: LocalDateTime? = null,
)

/**
 * 玩家领取记录
 */
@Entity
@Table(name = "fox_coins")
data class FoxCoins(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    /**
     * 领取时间
     */
    var time: LocalDateTime? = null,

    /**
     * 领取次数
     */
    var timer: Int? = null,
)