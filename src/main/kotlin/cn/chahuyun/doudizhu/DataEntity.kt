package cn.chahuyun.doudizhu

import cn.chahuyun.economy.entity.UserInfo
import jakarta.persistence.*
import java.util.*

/**
 * 玩家信息
 */
@Entity
@Table(name = "fox_user")
data class FoxUser(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    @OneToOne(mappedBy = "user")
    var user: UserInfo? = null,

    /**
     * 玩家id qq
     */
    var uid: Long? = null,
    /**
     * 玩家名称
     */
    var name: String? = null,
    /**
     * 玩家积分
     */
    var score: Int? = null,
    /**
     * 玩家胜利次数
     */
    var victory: Int? = null,
    /**
     * 玩家失败次数
     */
    var lose: Int? = null,
)

/**
 * 玩家辅主工具
 */
@Entity
@Table(name = "fox_game")
data class FoxGameAuxiliary(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    @OneToOne(mappedBy = "fox_user")
    var foxUser: FoxUser? = null,

    /**
     * 记牌器
     */
    var aScorer: Boolean? = null,

    /**
     * 记牌器时效
     */
    var aScorerTime: Date? = null,

    /**
     * 超级加倍特权
     */
    var superDouble: Boolean? = null,

    /**
     * 超级加倍特权时效
     */
    var superDoubleTime: Date? = null,
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

    @OneToOne(mappedBy = "fox_user")
    var foxUser: FoxUser? = null,

    /**
     * 领取时间
     */
    var time: Date? = null,

    /**
     * 领取次数
     */
    var timer: Int? = null,
)