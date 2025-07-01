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
     * 下一位出牌玩家
     */
    var nextPlayer: Player,
    /**
     * 剩余牌数
     */
    var residual: Int = 54,
) {
    /**
     * 地主
     */
    lateinit var landlord: Player

    /**
     * 给这个玩家加一张牌
     */
    fun addHand(playerIndex: Int, car: Car) {
        players[playerIndex].addHand(car)
    }

}

/**
 * 玩家
 */
data class Player(
    val id: Long,
    val name: String,
    /**
     * 手牌
     */
    val hand: MutableList<Cards> = mutableListOf(),
) {
    fun addHand(car: Car) {
        hand.find { it.car == car }?.let { it.num++ } ?: run { hand.add(Cards(car)) }
    }


    fun toHand(): String {
        // 定义排序规则
        val order = listOf(
            Car.THREE, Car.FOUR, Car.FIVE, Car.SIX, Car.SEVEN,
            Car.EIGHT, Car.NINE, Car.TEN, Car.J, Car.Q, Car.K,
            Car.A, Car.TWO, Car.SMALL_JOKER, Car.BIG_JOKER
        )

        // 对手牌进行排序
        val sortedHand = hand.sortedWith(compareBy { order.indexOf(it.car) })

        // 将手牌转换成字符串表示形式
        return sortedHand.flatMap { card ->
            List(card.num) { "[" + card.car.marking + "]" }
        }.joinToString("")
    }
}

/**
 * 手牌
 */
data class Cards(
    val car: Car,
    var num: Int = 1,
) {
    companion object {
        /**
         * 创建一副牌
         */
        @JvmStatic
        fun createFullDeck(): List<Cards> {
            return listOf(
                Cards(Car.A, 4),
                Cards(Car.TWO, 4),
                Cards(Car.THREE, 4),
                Cards(Car.FOUR, 4),
                Cards(Car.FIVE, 4),
                Cards(Car.SIX, 4),
                Cards(Car.SEVEN, 4),
                Cards(Car.EIGHT, 4),
                Cards(Car.NINE, 4),
                Cards(Car.TEN, 4),
                Cards(Car.J, 4),
                Cards(Car.Q, 4),
                Cards(Car.K, 4),
                Cards(Car.SMALL_JOKER, 1),
                Cards(Car.BIG_JOKER, 1)
            )
        }

        /**
         * 创建一副随机打乱展开的牌
         */
        @JvmStatic
        fun createFullExpandDeck(): List<Car> {
            return createFullDeck().flatMap { cars -> List(cars.num) { cars.car } }.shuffled()
        }
    }
}

/**
 * 牌
 */
enum class Car(
    val value: Int,
    val marking: String,
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