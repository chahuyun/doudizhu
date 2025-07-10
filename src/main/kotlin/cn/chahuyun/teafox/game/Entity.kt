package cn.chahuyun.teafox.game

import cn.chahuyun.teafox.game.util.CardUtil.show
import cn.chahuyun.teafox.game.util.RankUtils
import java.awt.Color
import java.awt.Color.BLACK
import java.awt.Color.RED


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
    fun addHand(playerIndex: Int, car: Card) {
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
     */
    fun canPlayCards(cards: List<Card>) =
        RankUtils.canPlayCards(hand, cards)

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
}

/**
 * 手牌
 */
data class CardRanks(
    /**
     * 牌
     */
    val car: Card,
    /**
     * 数量
     */
    var num: Int = 1,
) {
    companion object {
        /**
         * 创建一副无花色的牌
         */
        @JvmStatic
        fun createFullDeck(): List<CardRanks> {
            return listOf(
                CardRanks(Card(CardRank.ACE, CardColor.HEARTS), 4),
                CardRanks(Card(CardRank.TWO, CardColor.HEARTS), 4),
                CardRanks(Card(CardRank.THREE, CardColor.HEARTS), 4),
                CardRanks(Card(CardRank.FOUR, CardColor.HEARTS), 4),
                CardRanks(Card(CardRank.FIVE, CardColor.HEARTS), 4),
                CardRanks(Card(CardRank.SIX, CardColor.HEARTS), 4),
                CardRanks(Card(CardRank.SEVEN, CardColor.HEARTS), 4),
                CardRanks(Card(CardRank.EIGHT, CardColor.HEARTS), 4),
                CardRanks(Card(CardRank.NINE, CardColor.HEARTS), 4),
                CardRanks(Card(CardRank.TEN, CardColor.HEARTS), 4),
                CardRanks(Card(CardRank.JACK, CardColor.HEARTS), 4),
                CardRanks(Card(CardRank.QUEEN, CardColor.HEARTS), 4),
                CardRanks(Card(CardRank.KING, CardColor.HEARTS), 4),
                CardRanks(Card(CardRank.SMALL_JOKER, CardColor.HEARTS), 1),
                CardRanks(Card(CardRank.BIG_JOKER, CardColor.HEARTS), 1)
            )
        }

        /**
         * 创建一副无花色的牌
         */
        @JvmStatic
        fun createFullFourColorDeck(): List<CardRanks> {
            return listOf(
                createFourColorDeck(CardRank.ACE),
                createFourColorDeck(CardRank.TWO),
                createFourColorDeck(CardRank.THREE),
                createFourColorDeck(CardRank.FOUR),
                createFourColorDeck(CardRank.FIVE),
                createFourColorDeck(CardRank.SIX),
                createFourColorDeck(CardRank.SEVEN),
                createFourColorDeck(CardRank.EIGHT),
                createFourColorDeck(CardRank.NINE),
                createFourColorDeck(CardRank.TEN),
                createFourColorDeck(CardRank.JACK),
                createFourColorDeck(CardRank.QUEEN),
                createFourColorDeck(CardRank.KING),
                CardRanks(Card(CardRank.SMALL_JOKER, CardColor.HEARTS), 1),
                CardRanks(Card(CardRank.BIG_JOKER, CardColor.HEARTS), 1)
            )
        }

        /**
         * 创建一副随机打乱展开的牌
         */
        @JvmStatic
        fun createFullExpandDeck(): List<Card> {
            return createFullDeck().flatMap { cars -> List(cars.num) { cars.car } }.shuffled()
        }

        /**
         * 创建四色牌
         */
        @JvmStatic
        fun createFourColorDeck(rank: CardRank): List<Card> {
            return listOf(
                Card(rank, CardColor.HEARTS),
                Card(rank, CardColor.SPADES),
                Card(rank, CardColor.DIAMONDS),
                Card(rank, CardColor.CLUBS)
            )
        }
    }
}


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

    override fun toString(): String {
        return when (rank) {
            CardRank.SMALL_JOKER, CardRank.BIG_JOKER -> "[${rank.display}]"
            else -> "[${color.symbol}${rank.display}]"
        }
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
    BLACK_JOKER("黑王", 6, BLACK)
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
    DIZHU
}