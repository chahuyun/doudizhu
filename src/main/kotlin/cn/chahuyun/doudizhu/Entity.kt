package cn.chahuyun.doudizhu

/**
 * 对局
 */
data class Game(
    /**
     * 玩家
     */
    val players: List<Player>,
    /**
     * 群
     */
    val group: Long,
    /**
     * 底分
     */
    val bottom: Int,
    /**
     * 倍数
     */
    var fold: Int,
    /**
     * 剩余牌数
     */
    var residual: Int = 52,
    /**
     * 下一位出牌玩家
     */
    var nextPlayer: Player,
    /**
     * 地主
     */
    var landlord: Player,
)

/**
 * 玩家
 */
data class Player(
    val id: Long,
    val name: String,
    val hand: MutableList<Cards>,
)

/**
 * 手牌
 */
data class Cards(
    val car: Car,
    var num: Int,
)

/**
 * 牌
 */
enum class Car(
    val value: Int,
    val marking: String
) {
    A(1, "A"),
    TWO(2, "2"),
    THREE(3, "3"),
    FOUR(4, "4"),
    FIVE(5, "5"),
    SIX(6, "6"),
    SEVEN(7, "7"),
    EIGHT(8, "8"),
    NINE(9, "9"),
    TEN(10, "10"),
    J(11, "J"),
    Q(12, "Q"),
    K(13, "K"),
    // 添加大小王，通常它们没有数值，或者可以根据游戏规则给予特定值
    SMALL_JOKER(-1, "小王"),  // 小王
    BIG_JOKER(-2, "大王");      // 大王

    companion object {
        // 如果需要根据字符串查找对应的枚举成员，可以提供一个辅助方法
        fun fromMarking(marking: String): Car? {
            return values().find { it.marking.equals(marking, ignoreCase = true) }
        }
    }
}