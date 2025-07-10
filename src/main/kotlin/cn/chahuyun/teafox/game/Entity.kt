package cn.chahuyun.teafox.game

import cn.chahuyun.teafox.game.util.CardUtil.show
import cn.chahuyun.teafox.game.util.CardUtil.toListCar


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
    var residual: Int = 54,
    /**
     * 当前轮到哪个玩家出牌（索引）
     */
    var currentPlayerIndex: Int = 0,
    /**
     * 赢家
     */
    var winPlayer: MutableList<Player> = mutableListOf()
) {

    /**
     * 地主
     */
    lateinit var landlord: Player

    /**
     * 下一位出牌玩家
     */
    val nextPlayer: Player
        get() {
            for (i in 1..players.size) {
                val index = (currentPlayerIndex++ + i) % players.size
                val player = players[index]
                if (player.hand.isNotEmpty()) {
                    return player
                }
            }
            return players.find { it.hand.isNotEmpty() } ?: players[0]
        }

    /**
     * 手动设置下一个出牌人
     */
    @Suppress("AssignedValueIsNeverRead")
    fun setNextPlayer(player: Player) {
        var index = players.indexOf(player)
        if (index >= 0) {
            currentPlayerIndex = --index
        } else {
            error("Player not found in the game.")
        }
    }


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

    /**
     * 是否允许出牌（可用于托管、超时等情况）
     */
    var canPlay: Boolean = true,
) {
    fun addHand(car: Car) {
        hand.find { it.car == car }?.let { it.num++ } ?: run { hand.add(Cards(car)) }
    }

    /**
     * 平展牌打印
     */
    fun toHand(): String {
        return hand.show()
    }

    /**
     * 检查手牌是否足够出牌
     * @return true 够出
     */
    fun checkHand(cards: List<Car>): Boolean {
        // 将手牌转换为计数映射
        val handCountMap = hand.associate { it.car to it.num }

        // 将要出的牌分组计数
        val playCountMap = cards.groupingBy { it }.eachCount()

        // 检查每种牌的数量是否足够
        return playCountMap.all { (car, count) ->
            handCountMap[car]?.let { it >= count } ?: false
        }
    }

    /**
     * 出牌 - 从手牌中移除指定的牌
     * @param cardsToPlay 要出的牌
     * @return 出牌是否成功
     */
    fun playCards(cardsToPlay: List<Cards>): Boolean {
        // 先检查手牌是否足够
        if (!checkHand(cardsToPlay.toListCar())) {
            return false
        }

        // 从手牌中移除牌
        cardsToPlay.forEach { cardToPlay ->
            val handCard = hand.find { it.car == cardToPlay.car }
            if (handCard != null) {
                handCard.num -= cardToPlay.num

                // 如果数量减到0以下或等于0，从手牌中移除
                if (handCard.num <= 0) {
                    hand.remove(handCard)
                }
            }
        }

        return true
    }
}

/**
 * 手牌
 */
data class Cards(
    /**
     * 牌
     */
    val car: Car,
    /**
     * 数量
     */
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
    val sort: Int,
) {
    TWO(2, "2", 13),
    THREE(3, "3", 1),
    FOUR(4, "4", 2),
    FIVE(5, "5", 3),
    SIX(6, "6", 4),
    SEVEN(7, "7", 5),
    EIGHT(8, "8", 6),
    NINE(9, "9", 7),
    TEN(10, "10", 8),
    J(11, "J", 9),
    Q(12, "Q", 10),
    K(13, "K", 11),
    A(14, "A", 12),

    // 添加大小王，通常它们没有数值，或者可以根据游戏规则给予特定值
    SMALL_JOKER(-1, "小王", 14),  // 小王
    BIG_JOKER(-2, "大王", 15);      // 大王

    companion object {
        // 如果需要根据字符串查找对应的枚举成员，可以提供一个辅助方法
        fun fromMarking(marking: String): Car? {
            val normalized = marking.trim().uppercase()
            return entries.find {
                it.marking == normalized
            }
        }
    }

    override fun toString(): String {
        return "[$marking]"
    }


}

/**
 * 游戏桌对局大小类型
 */
enum class GameTableCoinsType(
    val min: Int, val max: Int,
    /**
     * 保底
     */
    val guaranteed: Int
) {
    NORMAL(10, 50, 100),
    BIG(50, 200, 400),
    HUGE(200, 1000, 2000),
    PEAK(1000, 3000, 5000)
}

/**
 * 游戏类型
 */
enum class GameType {
    /**
     * 斗地主
     */
    DIZHU
}