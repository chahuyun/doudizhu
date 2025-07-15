package cn.chahuyun.teafox.game

import cn.chahuyun.teafox.game.util.CardUtil.show
import cn.chahuyun.teafox.game.util.RankUtils
import java.awt.Color
import java.awt.Color.BLACK
import java.awt.Color.RED




/**
 * 玩家
 */
data class Player(
    val id: Long,
    val name: String,
    /**
     * 手牌
     */
    val hand: MutableList<Card> = mutableListOf(),

    /**
     * 是否允许出牌（可用于托管、超时等情况）
     */
    var canPlay: Boolean = true,
) {
    fun addHand(car: Card) {
        hand.add(car)
    }

    /**
     * 平展牌打印
     */
    fun toHand(): String {
        return hand.show()
    }

    /**
     * 检查是否可以出牌
     * @param cards 要出的牌
     * @param ignoreColor 是否忽略花色
     * @return true 可以出牌
     */
    fun canPlayCards(cards: List<Card>,ignoreColor: Boolean = true) : Boolean{
        return if (ignoreColor){
            RankUtils.canPlayCards(hand, cards)
        }else{
            cards.all { it-> hand.contains(it) }
        }
    }

    /**
     * 检查是否可以出牌
     * @param cards 要出的牌
     * @return true 可以出牌
     */
    fun canPlayCards(cards: List<CardRank>) : Boolean{
        return canPlayCards(cards.map { Card(it, CardColor.NO_FIT) })
    }

    /**
     * 出牌
     */
    fun playCards(cards: List<Card>): Boolean {
        return RankUtils.removeCards(hand, cards)
    }

    /**
     * 添加手牌
     */
    fun addCard(card: Card) {
        hand.add(card)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Player

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }


}

/**
 * 牌组
 */
data class CardGroup(
    val rank: CardRank,
    val num:Int,
    val cards:List<Card>
)

/**
 * 单张牌
 */
data class Card(
    val rank: CardRank,
    val color: CardColor
) {
    companion object {
        /**
         * 寻找牌面值
         */
        @JvmStatic
        fun fromMarking(display: String): CardRank? {
            val normalized = display.trim().uppercase()
            return CardRank.entries.find {
                it.display == normalized
            }
        }
    }

    /**
     * 打印，默认携带花色
     */
    override fun toString(): String {
        return when (rank) {
            CardRank.SMALL_JOKER, CardRank.BIG_JOKER -> "[${rank.display}]"
            else -> "[${color.symbol}${rank.display}]"
        }
    }

    /**
     * 打印，不带花色
     */
    fun toShow():String {
        return "[${rank.display}]"
    }
}


/**
 * 花色
 */
enum class CardColor(val symbol: String, val value: Int, val color: Color) {
    /**
     * 红心
     */
    HEARTS("♥", 1, RED),

    /**
     * 黑桃
     */
    SPADES("♠", 2, BLACK),

    /**
     * 方块
     */
    DIAMONDS("♦", 3, RED),

    /**
     * 梅花
     */
    CLUBS("♣", 4, BLACK),

    /**
     * 红王
     */
    RED_JOKER("红王", 5, RED),

    /**
     * 黑王
     */
    BLACK_JOKER("黑王", 6, BLACK),

    /**
     * 无花色
     */
    NO_FIT("", 0, Color.WHITE)
}

/**
 * 牌面值
 */
enum class CardRank(val value: Int, val display: String, val sort: Int) {
    THREE(3, "3", 1),
    FOUR(4, "4", 2),
    FIVE(5, "5", 3),
    SIX(6, "6", 4),
    SEVEN(7, "7", 5),
    EIGHT(8, "8", 6),
    NINE(9, "9", 7),
    TEN(10, "10", 8),
    JACK(11, "J", 9),
    QUEEN(12, "Q", 10),
    KING(13, "K", 11),
    ACE(14, "A", 12),
    TWO(15, "2", 13),
    SMALL_JOKER(16, "小王", 14),
    BIG_JOKER(17, "大王", 15)


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
    DIZHU,

    /**
     * 干瞪眼
     */
    GAND,
    /**
     * 羊狼棋
     * */
    BaghChalGame,
}