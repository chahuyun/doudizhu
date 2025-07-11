package cn.chahuyun.teafox.game.game

import cn.chahuyun.teafox.game.Card
import cn.chahuyun.teafox.game.CardGroup
import cn.chahuyun.teafox.game.CardRank
import cn.chahuyun.teafox.game.util.CardUtil.containsRank
import cn.chahuyun.teafox.game.util.CardUtil.sortNum
import cn.chahuyun.teafox.game.util.CardUtil.continuous
import cn.chahuyun.teafox.game.util.CardUtil.sortRank
import cn.chahuyun.teafox.game.util.CardUtil.toGroup

/**
 * 牌型
 */
enum class CardForm(val value: Int) {
    /**
     * 单牌
     */
    SINGLE(1),

    /**
     * 对子
     */
    PAIR(1),

    /**
     * 三张
     */
    TRIPLE(1),

    /**
     * 三带一
     */
    TRIPLE_ONE(1),

    /**
     * 三带一对
     */
    TRIPLE_TWO(1),

    /**
     * 四带二
     */
    FOUR_TWO(1),

    /**
     * 四带两对
     */
    FOUR_TWO_PAIR(1),

    /**
     * 飞机
     */
    AIRCRAFT(1),

    /**
     * 飞机带单
     */
    AIRCRAFT_SINGLE(1),

    /**
     * 飞机带对
     */
    AIRCRAFT_PAIR(1),

    /**
     * 顺子
     */
    QUEUE(1),

    /**
     * 连对
     */
    QUEUE_TWO(1),

    /**
     * 炸弹
     */
    BOMB(2),

    /**
     * 王炸
     */
    GHOST_BOMB(3),

    /**
     * 错误
     */
    ERROR(0)
}

object CardFormUtil {
    /**
     * 匹配牌型
     * 穷举类型，利用牌数量和牌型数量2维定位，快速匹配牌型
     * 穷尽了！
     */
    fun match(cards: List<CardGroup>): CardForm {
        //排序
        val sort = cards.sortNum()
        //总牌数量
        val sizeNum = cards.sumOf { it.num }
        //牌型数量
        val typeNum = sort.size
        return when (sizeNum) {
            1 -> CardForm.SINGLE
            2 -> when (typeNum) {
                1 -> CardForm.PAIR
                2 -> if (sort.containsRank(CardRank.SMALL_JOKER) && sort.containsRank(CardRank.BIG_JOKER)) CardForm.GHOST_BOMB
                else CardForm.ERROR

                else -> CardForm.ERROR
            }

            3 -> if (typeNum == 1) CardForm.TRIPLE
            else CardForm.ERROR

            4 -> when (typeNum) {
                1 -> CardForm.BOMB
                2 -> if (sort.inspection(3, 1)) CardForm.TRIPLE_ONE else CardForm.ERROR
                else -> CardForm.ERROR
            }

            5 -> when (typeNum) {
                2 -> if (sort.inspection(3, 2)) CardForm.TRIPLE_TWO else CardForm.ERROR
                5 -> if (sort.continuous()) CardForm.QUEUE else CardForm.ERROR
                else -> CardForm.ERROR
            }

            6 -> when (typeNum) {
                3 -> when {
                    sort.inspection(4, 1, 1) -> CardForm.FOUR_TWO
                    sort.continuous() && sort.inspection(2, 2, 2) -> CardForm.QUEUE_TWO
                    else -> CardForm.ERROR
                }

                6 -> if (sort.continuous()) CardForm.QUEUE else CardForm.ERROR
                else -> CardForm.ERROR
            }

            7 -> if (typeNum == 7) {
                if (sort.continuous()) CardForm.QUEUE else CardForm.ERROR
            } else CardForm.ERROR

            8 -> when (typeNum) {
                2 -> if (sort.continuous() && sort.inspection(4, 4)) CardForm.AIRCRAFT_SINGLE else CardForm.ERROR
                3 -> when {
                    sort.inspection(4, 2, 2) -> CardForm.FOUR_TWO_PAIR
                    sort.subList(0, 2).continuous() && (sort.inspection(3, 3, 2) ||
                            sort.inspection(4, 3, 1)) -> CardForm.AIRCRAFT_SINGLE

                    else -> CardForm.ERROR
                }

                4 -> when {
                    sort.continuous() && sort.inspection(2, 2, 2, 2) -> CardForm.QUEUE_TWO
                    sort.inspection(3, 3, 1, 1) -> if (sort.subList(0, 2).continuous())
                        CardForm.AIRCRAFT_SINGLE else CardForm.ERROR

                    else -> CardForm.ERROR
                }

                8 -> if (sort.continuous()) CardForm.QUEUE else CardForm.ERROR
                else -> CardForm.ERROR
            }

            9 -> when (typeNum) {
                3 -> if (sort.continuous()) CardForm.AIRCRAFT else CardForm.ERROR
                9 -> if (sort.continuous()) CardForm.QUEUE else CardForm.ERROR
                else -> CardForm.ERROR
            }

            10 -> when (typeNum) {
                4 -> if (sort.subList(0, 2).continuous() &&
                    sort.inspection(3, 3, 2, 2)) CardForm.AIRCRAFT_PAIR else CardForm.ERROR

                5 -> if (sort.continuous() && sort.inspection(2, 2, 2, 2, 2)) CardForm.QUEUE_TWO else CardForm.ERROR
                10 -> if (sort.continuous()) CardForm.QUEUE else CardForm.ERROR
                else -> CardForm.ERROR
            }

            11 -> if (typeNum == 11) {
                if (sort.continuous()) CardForm.QUEUE else CardForm.ERROR
            } else CardForm.ERROR

            12 -> when (typeNum) {
                3 -> if (sort.continuous() && sort.inspection(4, 4, 4)) CardForm.AIRCRAFT_SINGLE else CardForm.ERROR
                4 -> when {
                    sort.continuous() && sort.inspection(3, 3, 3, 3) -> CardForm.AIRCRAFT
                    sort.subList(0, 4).continuous() && (sort.inspection(3, 3, 3, 3) ||
                            sort.inspection(4, 3, 3, 2)) -> CardForm.AIRCRAFT_SINGLE

                    else -> CardForm.ERROR
                }

                5 -> if (sort.subList(0, 4).continuous() && sort.inspection(4, 3, 3, 1, 1)
                ) CardForm.AIRCRAFT_SINGLE else CardForm.ERROR

                6 -> when {
                    sort.continuous() && sort.inspection(2, 2, 2, 2, 2, 2) -> CardForm.QUEUE_TWO
                    sort.subList(0, 4).continuous() && sort.inspection(3, 3, 3, 1, 1, 1) -> CardForm.AIRCRAFT_SINGLE
                    else -> CardForm.ERROR
                }

                12 -> if (sort.continuous()) CardForm.QUEUE else CardForm.ERROR
                else -> CardForm.ERROR
            }

            14 -> if (typeNum == 7 && sort.continuous() && sort.inspection(2, 2, 2, 2, 2, 2, 2)) CardForm.QUEUE_TWO else CardForm.ERROR

            15 -> when (typeNum) {
                6 -> if (sort.subList(0, 4).continuous() && sort.inspection(3, 3, 3, 2, 2, 2)) CardForm.AIRCRAFT_SINGLE else CardForm.ERROR

                else -> CardForm.ERROR
            }

            16 -> when (typeNum) {
                4 -> if (sort.continuous() && sort.inspection(4, 4, 4, 4)) CardForm.AIRCRAFT_SINGLE else CardForm.ERROR
                5 -> if (sort.subList(0, 5).continuous() && (sort.inspection(4, 3, 3, 3, 3) || sort.inspection(4, 4, 4, 3, 1))
                ) CardForm.AIRCRAFT_SINGLE else CardForm.ERROR

                6 -> when {
                    sort.subList(0, 5).continuous() && (
                            sort.inspection(3, 3, 3, 3, 3, 1) || sort.inspection(3, 3, 3, 3, 2, 2) ||
                                    sort.inspection(4, 3, 3, 3, 2, 1) || sort.inspection(4, 4, 3, 3, 1, 1)
                            ) -> CardForm.AIRCRAFT_SINGLE

                    else -> CardForm.ERROR
                }

                7 -> if (sort.subList(0, 5).continuous() && sort.inspection(3, 3, 3, 3, 2, 1, 1)
                ) CardForm.AIRCRAFT_SINGLE
                else CardForm.ERROR

                8 -> when {
                    sort.continuous() && sort.inspection(2, 2, 2, 2, 2, 2, 2, 2) -> CardForm.QUEUE_TWO
                    sort.subList(0, 5).continuous() && sort.inspection(3, 3, 3, 3, 1, 1, 1, 1) -> CardForm.AIRCRAFT_SINGLE

                    else -> CardForm.ERROR
                }

                else -> CardForm.ERROR
            }

            18 -> if (typeNum == 9 && sort.continuous() && sort.inspection(2, 2, 2, 2, 2, 2, 2, 2, 2)
            ) CardForm.QUEUE_TWO else CardForm.ERROR

            20 -> when (typeNum) {
                5 -> if (sort.continuous() && sort.inspection(4, 4, 4, 4)) CardForm.AIRCRAFT_SINGLE else CardForm.ERROR
                6 -> when {
                    sort.subList(0, 6).continuous() && sort.inspection(4, 4, 4, 4, 3, 1) -> CardForm.AIRCRAFT_SINGLE
                    sort.subList(0, 5).continuous() && sort.inspection(4, 4, 3, 3, 3, 3) -> CardForm.AIRCRAFT_PAIR
                    else -> CardForm.ERROR
                }

                7 -> if (sort.subList(0, 6).continuous() && (
                            sort.inspection(4, 4, 4, 3, 3, 1, 1) || sort.inspection(4, 4, 3, 3, 3, 2, 1) ||
                                    sort.inspection(4, 3, 3, 3, 3, 3, 1)
                            )
                ) CardForm.AIRCRAFT_SINGLE else CardForm.ERROR

                8 -> when {
                    sort.subList(0, 5).continuous() && sort.inspection(4, 3, 3, 3, 3, 2, 2) -> CardForm.AIRCRAFT_PAIR
                    sort.subList(0, 6).continuous() && (
                            sort.inspection(4, 4, 3, 3, 3, 1, 1, 1) || sort.inspection(4, 3, 3, 3, 3, 2, 1, 1) ||
                                    sort.inspection(3, 3, 3, 3, 3, 3, 1, 1) || sort.inspection(3, 3, 3, 3, 3, 2, 2, 1)
                            ) -> CardForm.AIRCRAFT_SINGLE

                    else -> CardForm.ERROR
                }

                9 -> if (sort.subList(0, 6).continuous() && (sort.inspection(
                        4, 3, 3, 3, 3, 1, 1, 1, 1
                ) || sort.inspection(3, 3, 3, 3, 3, 2, 1, 1, 1))
                ) CardForm.AIRCRAFT_SINGLE else CardForm.ERROR

                10 -> when {
                    sort.continuous() && sort.inspection(2, 2, 2, 2, 2, 2, 2, 2, 2, 2) -> CardForm.QUEUE_TWO
                    sort.subList(0, 6).continuous() && sort.inspection(3, 3, 3, 3, 3, 1, 1, 1, 1, 1
                    ) -> CardForm.AIRCRAFT_SINGLE

                    else -> CardForm.ERROR
                }

                else -> CardForm.ERROR
            }

            else -> CardForm.ERROR
        }
    }

    /**
     * 检查能不能吃的起，基于牌值
     * 仅限斗地主
     * @return true 吃得起
     */
    fun CardForm.check(max: List<Card>, now: List<Card>): Boolean {
        val maxGroup = max.toGroup()
        val nowGroup = now.toGroup()
        return when (this) {
            CardForm.SINGLE -> nowGroup.getMaxValue() > maxGroup.getMaxValue()
            CardForm.PAIR -> nowGroup.getMaxValue() > maxGroup.getMaxValue()
            CardForm.TRIPLE -> nowGroup.sortNum().getMaxValue() > maxGroup.sortNum().getMaxValue()
            CardForm.TRIPLE_ONE -> nowGroup.sortNum().getMaxValue() > maxGroup.sortNum().getMaxValue()
            CardForm.TRIPLE_TWO -> nowGroup.sortNum().getMaxValue() > maxGroup.sortNum().getMaxValue()
            CardForm.QUEUE -> if (maxGroup.size != nowGroup.size) return false else
                nowGroup.sortRank().getMaxValue() > maxGroup.sortRank().getMaxValue()

            CardForm.QUEUE_TWO -> if (maxGroup.size != nowGroup.size) return false else
                nowGroup.sortRank().getMaxValue() > maxGroup.sortRank().getMaxValue()

            CardForm.BOMB -> nowGroup.getMaxValue() > maxGroup.getMaxValue()
            CardForm.AIRCRAFT -> nowGroup.sortRank().getMaxValue() > maxGroup.sortRank().getMaxValue()
            //todo 这里有bug，飞机长度没有判断
            CardForm.AIRCRAFT_SINGLE ->
                nowGroup.sortNum().filter { it.num != 1 }.sortRank().getMaxValue() > maxGroup.sortNum().filter { it.num != 1 }.sortRank()
                    .getMaxValue()

            CardForm.AIRCRAFT_PAIR ->
                nowGroup.sortNum().filter { it.num != 2 }.sortRank().getMaxValue() > maxGroup.sortNum().filter { it.num != 2 }.sortRank()
                    .getMaxValue()

            CardForm.FOUR_TWO -> nowGroup.sortNum().getMaxValue() > maxGroup.sortNum().getMaxValue()
            CardForm.FOUR_TWO_PAIR -> nowGroup.sortNum().sortRank().getMaxValue() > maxGroup.sortNum().sortRank().getMaxValue()
            else -> false
        }
    }

    /**
     * 获取当前牌堆第一种牌的值
     */
    private fun List<CardGroup>.getMaxValue(): Int {
        return this.first().rank.value
    }

    /**
     * 根据顺序判断手牌数量
     */
    private fun List<CardGroup>.inspection(vararg nums: Int): Boolean =
        size == nums.size && indices.all { this[it].num == nums[it] }
}

