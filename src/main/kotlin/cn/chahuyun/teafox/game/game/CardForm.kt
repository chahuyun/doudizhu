package cn.chahuyun.teafox.game.game

import cn.chahuyun.teafox.game.Car
import cn.chahuyun.teafox.game.Cards
import cn.chahuyun.teafox.game.util.CardUtil.sortNum
import cn.chahuyun.teafox.game.util.CardUtil.contains
import cn.chahuyun.teafox.game.util.CardUtil.continuous
import cn.chahuyun.teafox.game.util.CardUtil.sort

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
    fun match(cards: List<Cards>): CardForm {
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
                2 -> if (sort.contains(Car.SMALL_JOKER) && sort.contains(Car.BIG_JOKER)) CardForm.GHOST_BOMB
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
                5 -> if (continuous(sort)) CardForm.QUEUE else CardForm.ERROR
                else -> CardForm.ERROR
            }

            6 -> when (typeNum) {
                3 -> when {
                    sort.inspection(4, 1, 1) -> CardForm.FOUR_TWO
                    continuous(sort) && sort.inspection(2, 2, 2) -> CardForm.QUEUE_TWO
                    else -> CardForm.ERROR
                }

                6 -> if (continuous(sort)) CardForm.QUEUE else CardForm.ERROR
                else -> CardForm.ERROR
            }

            7 -> if (typeNum == 7) {
                if (continuous(sort)) CardForm.QUEUE else CardForm.ERROR
            } else CardForm.ERROR

            8 -> when (typeNum) {
                2 -> if (continuous(sort) && sort.inspection(4, 4)) CardForm.AIRCRAFT_SINGLE else CardForm.ERROR
                3 -> when {
                    sort.inspection(4, 2, 2) -> CardForm.FOUR_TWO_PAIR
                    continuous(sort.subList(0, 2)) && (sort.inspection(3, 3, 2) || sort.inspection(
                        4,
                        3,
                        1
                    )) -> CardForm.AIRCRAFT_SINGLE

                    else -> CardForm.ERROR
                }

                4 -> when {
                    continuous(sort) && sort.inspection(2, 2, 2, 2) -> CardForm.QUEUE_TWO
                    sort.inspection(3, 3, 1, 1) -> if (continuous(sort.subList(0, 2)))
                        CardForm.AIRCRAFT_SINGLE else CardForm.ERROR

                    else -> CardForm.ERROR
                }

                8 -> if (continuous(sort)) CardForm.QUEUE else CardForm.ERROR
                else -> CardForm.ERROR
            }

            9 -> when (typeNum) {
                3 -> if (continuous(sort)) CardForm.AIRCRAFT else CardForm.ERROR
                9 -> if (continuous(sort)) CardForm.QUEUE else CardForm.ERROR
                else -> CardForm.ERROR
            }

            10 -> when (typeNum) {
                4 -> if (continuous(sort.subList(0, 2)) && sort.inspection(
                        3,
                        3,
                        2,
                        2
                    )
                ) CardForm.AIRCRAFT_PAIR else CardForm.ERROR

                5 -> if (continuous(sort) && sort.inspection(2, 2, 2, 2, 2)) CardForm.QUEUE_TWO else CardForm.ERROR
                10 -> if (continuous(sort)) CardForm.QUEUE else CardForm.ERROR
                else -> CardForm.ERROR
            }

            11 -> if (typeNum == 11) {
                if (continuous(sort)) CardForm.QUEUE else CardForm.ERROR
            } else CardForm.ERROR

            12 -> when (typeNum) {
                3 -> if (continuous(sort) && sort.inspection(4, 4, 4)) CardForm.AIRCRAFT_SINGLE else CardForm.ERROR
                4 -> when {
                    continuous(sort) && sort.inspection(3, 3, 3, 3) -> CardForm.AIRCRAFT
                    continuous(sort.subList(0, 4)) && (sort.inspection(3, 3, 3, 3) || sort.inspection(
                        4,
                        3,
                        3,
                        2
                    )) -> CardForm.AIRCRAFT_SINGLE

                    else -> CardForm.ERROR
                }

                5 -> if (continuous(sort.subList(0, 4)) && sort.inspection(
                        4,
                        3,
                        3,
                        1,
                        1
                    )
                ) CardForm.AIRCRAFT_SINGLE else CardForm.ERROR

                6 -> when {
                    continuous(sort) && sort.inspection(2, 2, 2, 2, 2, 2) -> CardForm.QUEUE_TWO
                    continuous(sort.subList(0, 4)) && sort.inspection(3, 3, 3, 1, 1, 1) -> CardForm.AIRCRAFT_SINGLE
                    else -> CardForm.ERROR
                }

                12 -> if (continuous(sort)) CardForm.QUEUE else CardForm.ERROR
                else -> CardForm.ERROR
            }

            14 -> if (typeNum == 7 && continuous(sort) && sort.inspection(
                    2,
                    2,
                    2,
                    2,
                    2,
                    2,
                    2
                )
            ) CardForm.QUEUE_TWO else CardForm.ERROR

            15 -> when (typeNum) {
                6 -> if (continuous(sort.subList(0, 4)) && sort.inspection(
                        3,
                        3,
                        3,
                        2,
                        2,
                        2
                    )
                ) CardForm.AIRCRAFT_SINGLE else CardForm.ERROR

                else -> CardForm.ERROR
            }

            16 -> when (typeNum) {
                4 -> if (continuous(sort) && sort.inspection(4, 4, 4, 4)) CardForm.AIRCRAFT_SINGLE else CardForm.ERROR
                5 -> if (continuous(sort.subList(0, 5)) && (sort.inspection(4, 3, 3, 3, 3) || sort.inspection(
                        4,
                        4,
                        4,
                        3,
                        1
                    ))
                ) CardForm.AIRCRAFT_SINGLE else CardForm.ERROR

                6 -> when {
                    continuous(sort.subList(0, 5)) && (
                            sort.inspection(3, 3, 3, 3, 3, 1) || sort.inspection(3, 3, 3, 3, 2, 2) ||
                                    sort.inspection(4, 3, 3, 3, 2, 1) || sort.inspection(4, 4, 3, 3, 1, 1)
                            ) -> CardForm.AIRCRAFT_SINGLE

                    else -> CardForm.ERROR
                }

                7 -> if (continuous(sort.subList(0, 5)) && sort.inspection(
                        3,
                        3,
                        3,
                        3,
                        2,
                        1,
                        1
                    )
                ) CardForm.AIRCRAFT_SINGLE
                else CardForm.ERROR

                8 -> when {
                    continuous(sort) && sort.inspection(2, 2, 2, 2, 2, 2, 2, 2) -> CardForm.QUEUE_TWO
                    continuous(sort.subList(0, 5)) && sort.inspection(
                        3,
                        3,
                        3,
                        3,
                        1,
                        1,
                        1,
                        1
                    ) -> CardForm.AIRCRAFT_SINGLE

                    else -> CardForm.ERROR
                }

                else -> CardForm.ERROR
            }

            18 -> if (typeNum == 9 && continuous(sort) && sort.inspection(
                    2,
                    2,
                    2,
                    2,
                    2,
                    2,
                    2,
                    2,
                    2
                )
            ) CardForm.QUEUE_TWO else CardForm.ERROR

            20 -> when (typeNum) {
                5 -> if (continuous(sort) && sort.inspection(4, 4, 4, 4)) CardForm.AIRCRAFT_SINGLE else CardForm.ERROR
                6 -> when {
                    continuous(sort.subList(0, 6)) && sort.inspection(4, 4, 4, 4, 3, 1) -> CardForm.AIRCRAFT_SINGLE
                    continuous(sort.subList(0, 5)) && sort.inspection(4, 4, 3, 3, 3, 3) -> CardForm.AIRCRAFT_PAIR
                    else -> CardForm.ERROR
                }

                7 -> if (continuous(sort.subList(0, 6)) && (
                            sort.inspection(4, 4, 4, 3, 3, 1, 1) || sort.inspection(4, 4, 3, 3, 3, 2, 1) ||
                                    sort.inspection(4, 3, 3, 3, 3, 3, 1)
                            )
                ) CardForm.AIRCRAFT_SINGLE else CardForm.ERROR

                8 -> when {
                    continuous(sort.subList(0, 5)) && sort.inspection(4, 3, 3, 3, 3, 2, 2) -> CardForm.AIRCRAFT_PAIR
                    continuous(sort.subList(0, 6)) && (
                            sort.inspection(4, 4, 3, 3, 3, 1, 1, 1) || sort.inspection(4, 3, 3, 3, 3, 2, 1, 1) ||
                                    sort.inspection(3, 3, 3, 3, 3, 3, 1, 1) || sort.inspection(3, 3, 3, 3, 3, 2, 2, 1)
                            ) -> CardForm.AIRCRAFT_SINGLE

                    else -> CardForm.ERROR
                }

                9 -> if (continuous(sort.subList(0, 6)) && (sort.inspection(
                        4,
                        3,
                        3,
                        3,
                        3,
                        1,
                        1,
                        1,
                        1
                    ) || sort.inspection(3, 3, 3, 3, 3, 2, 1, 1, 1))
                ) CardForm.AIRCRAFT_SINGLE else CardForm.ERROR

                10 -> when {
                    continuous(sort) && sort.inspection(2, 2, 2, 2, 2, 2, 2, 2, 2, 2) -> CardForm.QUEUE_TWO
                    continuous(sort.subList(0, 6)) && sort.inspection(
                        3,
                        3,
                        3,
                        3,
                        3,
                        1,
                        1,
                        1,
                        1,
                        1
                    ) -> CardForm.AIRCRAFT_SINGLE

                    else -> CardForm.ERROR
                }

                else -> CardForm.ERROR
            }

            else -> CardForm.ERROR
        }
    }

    /**
     * 检查能不能吃的起
     * @return true 吃得起
     */
    fun CardForm.check(max: List<Cards>, now: List<Cards>): Boolean {
        return when (this) {
            CardForm.SINGLE -> now.getMaxValue() > max.getMaxValue()
            CardForm.PAIR -> now.getMaxValue() > max.getMaxValue()
            CardForm.TRIPLE -> now.sortNum().getMaxValue() > max.sortNum().getMaxValue()
            CardForm.TRIPLE_ONE -> now.sortNum().getMaxValue() > max.sortNum().getMaxValue()
            CardForm.TRIPLE_TWO -> now.sortNum().getMaxValue() > max.sortNum().getMaxValue()
            CardForm.QUEUE -> if (max.size != now.size) return false else
                now.sort().getMaxValue() > max.sort().getMaxValue()

            CardForm.QUEUE_TWO -> if (max.size != now.size) return false else
                now.sort().getMaxValue() > max.sort().getMaxValue()

            CardForm.BOMB -> now.getMaxValue() > max.getMaxValue()
            CardForm.AIRCRAFT -> now.sort().getMaxValue() > max.sort().getMaxValue()
            //todo 这里有bug，飞机长度没有判断
            CardForm.AIRCRAFT_SINGLE ->
                now.sortNum().filter { it.num != 1 }.sort().getMaxValue() > max.sortNum().filter { it.num != 1 }.sort()
                    .getMaxValue()

            CardForm.AIRCRAFT_PAIR ->
                now.sortNum().filter { it.num != 2 }.sort().getMaxValue() > max.sortNum().filter { it.num != 2 }.sort()
                    .getMaxValue()

            CardForm.FOUR_TWO -> now.sortNum().getMaxValue() > max.sortNum().getMaxValue()
            CardForm.FOUR_TWO_PAIR -> now.sortNum().sort().getMaxValue() > max.sortNum().sort().getMaxValue()
            else -> false
        }
    }

    /**
     * 获取当前牌堆第一种牌的值
     */
    private fun List<Cards>.getMaxValue(): Int {
        return this.first().car.sort
    }

    /**
     * 根据顺序判断手牌数量
     */
    private fun List<Cards>.inspection(vararg nums: Int): Boolean =
        size == nums.size && indices.all { this[it].num == nums[it] }
}

