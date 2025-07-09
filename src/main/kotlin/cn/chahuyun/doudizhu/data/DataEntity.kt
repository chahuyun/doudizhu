package cn.chahuyun.doudizhu.data

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
){
    /**
     * 获取胜率
     */
    fun winRate():Int = (victory!!/((victory!!+lose!!)))
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