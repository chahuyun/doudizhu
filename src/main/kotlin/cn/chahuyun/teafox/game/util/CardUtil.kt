@file:Suppress("unused")

package cn.chahuyun.teafox.game.util

import cn.chahuyun.teafox.game.*

/**
 * 游戏牌工具
 */
object CardUtil {

    // == 打印 ==

    /**
     * 打印牌
     */
    fun List<Card>.show(showColor: Boolean = false): String {
        return sortRank().joinToString {
            if (showColor && it.color != CardColor.NO_FIT) {
                it.toString() // 带颜色输出
            } else {
                it.toShow() // 不带颜色输出
            }
        }
    }

    /**
     * 反方向打印牌
     */
    fun List<Card>.showDesc(showColor: Boolean = false): String {
        return sortRank().asReversed().joinToString {
            if (showColor && it.color != CardColor.NO_FIT) {
                it.toString()
            } else {
                it.toShow()
            }
        }
    }

    // == 转换 ==

    /**
     * 转换为牌组
     */
    fun List<Card>.toGroup(): List<CardGroup> {
        return groupBy { it.rank }
            .map { (rank, cards) -> CardGroup(rank, cards.size, cards) }
    }

    /**
     * 牌组转换为牌
     */
    fun List<CardGroup>.toCard(): List<Card> {
        return flatMap { it.cards }
    }

    /**
     * 将一组牌值转换为牌
     * 默认情况下都是无花色的牌
     * 也可以手动设置牌色
     * @param color 牌色
     */
    fun List<CardRank>.toCard(color: CardColor = CardColor.NO_FIT): List<Card> = map { Card(it, color) }

    // == 排序 ==

    /**
     * 按照斗地主的牌的大小进行排序
     */
    @JvmName("sortRankCard")
    fun List<Card>.sortRank(): List<Card> = sortedByDescending { it.rank.sort }

    /**
     * 按照斗地主的牌大小进行排序
     */
    @JvmName("sortRankCardGroup")
    fun List<CardGroup>.sortRank() = sortedByDescending { it.rank.sort }

    /**
     * 排序,数量多的在前
     */
    @JvmName("sortNumCard")
    fun List<Card>.sortNum(): List<CardGroup> = toGroup().sortedByDescending { it.num }

    @JvmName("sortNumCardGroup")
    fun List<CardGroup>.sortNum(): List<CardGroup> = sortedByDescending { it.num }

    // ==  获取 ==

    /**
     * 获取牌组中对应牌组
     * 基于牌值
     */
    fun List<CardGroup>.get(cardGroup: CardGroup): CardGroup? {
        return find { it.rank == cardGroup.rank }
    }

    /**
     * 获取牌组不同牌值的数量
     */
    fun List<Card>.getRankSize(): Int = toGroup().size

    /**
     * 牌值转换
     * 通过字符串识别排值
     */
    fun knowCardRank(str: String): CardRank = when (str.uppercase()) {
        "4" -> CardRank.FOUR
        "5" -> CardRank.FIVE
        "6" -> CardRank.SIX
        "7" -> CardRank.SEVEN
        "8" -> CardRank.EIGHT
        "9" -> CardRank.NINE
        "10" -> CardRank.TEN
        "J" -> CardRank.JACK
        "Q" -> CardRank.QUEEN
        "K" -> CardRank.KING
        "A" -> CardRank.ACE
        "2" -> CardRank.TWO
        "大王" -> CardRank.BIG_JOKER
        "小王" -> CardRank.SMALL_JOKER
        else -> CardRank.THREE
    }

    // == 创建工具 ==

    /**
     * 创建一副随机打乱展开的牌
     */
    @JvmStatic
    fun createFullExpandDeck(): List<Card> {
        val cards = ArrayList<Card>(54)
        for (rank in CardRank.entries) {
            when (rank) {
                CardRank.SMALL_JOKER -> cards.add(Card(rank, CardColor.BLACK_JOKER))
                CardRank.BIG_JOKER -> cards.add(Card(rank, CardColor.RED_JOKER))
                else -> cards.addAll(fourColor(rank))
            }
        }
        return cards.shuffled()
    }

    /**
     * 创建牌辅助工具：创建四色牌
     */
    fun fourColor(rank: CardRank): List<Card> {
        return listOf(
            Card(rank, CardColor.SPADES),
            Card(rank, CardColor.HEARTS),
            Card(rank, CardColor.CLUBS),
            Card(rank, CardColor.DIAMONDS)
        )
    }


    // == 其他工具 ==

    /**
     * 匹配牌
     */
    @JvmName("containsRankFormCard")
    fun List<Card>.containsRank(car: Card): Boolean = any { it.rank == car.rank }

    @JvmName("containsRankFormRank")
    fun List<Card>.containsRank(rank: CardRank): Boolean = any { it.rank == rank }

    @JvmName("containsGroupRankFormCard")
    fun List<CardGroup>.containsRank(car: Card): Boolean = any { it.rank == car.rank }

    @JvmName("containsGroupRankFormRank")
    fun List<CardGroup>.containsRank(rank: CardRank): Boolean = any { it.rank == rank }


    /**
     * 判断是否连续（允许乱序）
     * 全局排除大小王
     * 排除2（仅限斗地主）
     *
     * @param type GameType 游戏类型
     */
    @JvmName("continuousCar")
    fun List<Card>.continuous(type: GameType = GameType.DIZHU): Boolean {
        return toGroup().continuous(type)
    }

    /**
     * 判断是否连续（允许乱序）
     * 全局排除大小王
     * 排除2（仅限斗地主）
     *
     * @param type GameType 游戏类型
     */
    @JvmName("continuousCarGroup")
    fun List<CardGroup>.continuous(type: GameType = GameType.DIZHU): Boolean {
        if (size <= 1) return false

        //链子中不能有大小王和2
        if (containsRank(CardRank.SMALL_JOKER) || containsRank(CardRank.BIG_JOKER)) {
            return false
        }

        when (type) {
            GameType.DIZHU -> if (containsRank(CardRank.TWO)) return false
        }


        // 先根据 value 排序
        val sorted = sortedBy { it.rank.sort }

        // 再检查是否连续
        return sorted.zipWithNext().all { (car1, car2) ->
            car2.rank.value == car1.rank.value + 1
        }
    }


}