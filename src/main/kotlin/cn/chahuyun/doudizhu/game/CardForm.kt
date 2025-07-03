package cn.chahuyun.doudizhu.game

import cn.chahuyun.doudizhu.Car
import cn.chahuyun.doudizhu.Car.Companion.continuous
import cn.chahuyun.doudizhu.Cards

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

        /**
         * 排序
         */
        val sort = cards.sort()

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
                2->if(continuous(sort)&& sort.inspection(4,4)) CardForm.AIRCRAFT_SINGLE else CardForm.ERROR
                3 -> when {
                    sort.inspection(4, 2, 2) -> CardForm.FOUR_TWO_PAIR
                    continuous(sort.subList(0,2)) && (sort.inspection(3,3,2) ||sort.inspection(4,3,1) ) -> CardForm.AIRCRAFT_SINGLE
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
            10-> when (typeNum) {
                4-> if(continuous(sort.subList(0,2)) && sort.inspection(3,3,2,2)) CardForm.AIRCRAFT_PAIR else CardForm.ERROR
                5-> if (continuous(sort) && sort.inspection(2,2,2,2,2)) CardForm.QUEUE_TWO else CardForm.ERROR
                10 -> if (continuous(sort)) CardForm.QUEUE else CardForm.ERROR
                else -> CardForm.ERROR
            }
            11-> if (typeNum == 11) {
                if (continuous(sort)) CardForm.QUEUE else CardForm.ERROR
            } else CardForm.ERROR
            12-> when(typeNum){
                3->if (continuous(sort)&&sort.inspection(4,4,4)) CardForm.AIRCRAFT_SINGLE else CardForm.ERROR
                4-> when {
                    continuous(sort) && sort.inspection(3,3,3,3) -> CardForm.AIRCRAFT
                    continuous(sort.subList(0,4)) &&( sort.inspection(3,3,3,3)||sort.inspection(4,3,3,2) )-> CardForm.AIRCRAFT_SINGLE
                    else -> CardForm.ERROR
                }
                5->if (continuous(sort.subList(0,4)) && sort.inspection(4,3,3,1,1)) CardForm.AIRCRAFT_SINGLE else CardForm.ERROR
                6-> when {
                    continuous(sort) && sort.inspection(2,2,2,2,2,2) -> CardForm.QUEUE_TWO
                    continuous(sort.subList(0,4)) && sort.inspection(3,3,3,1,1,1) -> CardForm.AIRCRAFT_SINGLE
                    else -> CardForm.ERROR
                }
                12-> if (continuous(sort)) CardForm.QUEUE else CardForm.ERROR
                else -> CardForm.ERROR
            }
            15->when(typeNum){
                6-> if(continuous(sort.subList(0,4)) && sort.inspection(3,3,3,2,2,2)) CardForm.AIRCRAFT_SINGLE else CardForm.ERROR
                else-> CardForm.ERROR
            }
            16->when(typeNum){
                4->if(continuous(sort) && sort.inspection(4,4,4,4)) CardForm.AIRCRAFT_SINGLE else CardForm.ERROR
                5->if (continuous(sort.subList(0,5)) &&(sort.inspection(4,3,3,3,3) || sort.inspection(4,4,4,3,1))) CardForm.AIRCRAFT_SINGLE else CardForm.ERROR
                6-> when {
                    continuous(sort.subList(0,5)) &&(
                            sort.inspection(3,3,3,3,3,1) || sort.inspection(3,3,3,3,2,2) ||
                                    sort.inspection(4,3,3,3,2,1) || sort.inspection(4,4,3,3,1,1)
                            ) -> CardForm.AIRCRAFT_SINGLE
                    else -> CardForm.ERROR
                }
                7->if (continuous(sort.subList(0,5)) && sort.inspection(3,3,3,3,2,1,1)) CardForm.AIRCRAFT_SINGLE else CardForm.ERROR
                8->if (continuous(sort.subList(0,5)) && sort.inspection(3,3,3,3,1,1,1,1)) CardForm.AIRCRAFT_SINGLE else CardForm.ERROR
                else-> CardForm.ERROR
            }
            20->when(typeNum){
                5->if(continuous(sort) && sort.inspection(4,4,4,4)) CardForm.AIRCRAFT_SINGLE else CardForm.ERROR
                6-> when {
                    continuous(sort.subList(0,6)) && sort.inspection(4,4,4,4,3,1) -> CardForm.AIRCRAFT_SINGLE
                    continuous(sort.subList(0,5)) && sort.inspection(4,4,3,3,3,3) -> CardForm.AIRCRAFT_PAIR
                    else -> CardForm.ERROR
                }
                7-> if (continuous(sort.subList(0,6)) &&(
                             sort.inspection(4,4,4,3,3,1,1) || sort.inspection(4,4,3,3,3,2,1) ||
                                     sort.inspection(4,3,3,3,3,3,1)
                            )) CardForm.AIRCRAFT_SINGLE else CardForm.ERROR
                8-> when {
                    continuous(sort.subList(0,5)) && sort.inspection(4,3,3,3,3,2,2) -> CardForm.AIRCRAFT_PAIR
                    continuous(sort.subList(0,6)) && (
                            sort.inspection(4,4,3,3,3,1,1,1) || sort.inspection(4,3,3,3,3,2,1,1) ||
                            sort.inspection(3,3,3,3,3,3,1,1) || sort.inspection(3,3,3,3,3,2,2,1)
                            ) -> CardForm.AIRCRAFT_SINGLE
                    else -> CardForm.ERROR
                }
                9-> if (continuous(sort.subList(0,6)) && (sort.inspection(4,3,3,3,3,1,1,1,1)||sort.inspection(3,3,3,3,3,2,1,1,1))) CardForm.AIRCRAFT_SINGLE else CardForm.ERROR
                10-> if (continuous(sort.subList(0,6)) && sort.inspection(3,3,3,3,3,1,1,1,1,1)) CardForm.AIRCRAFT_SINGLE else CardForm.ERROR
                else -> CardForm.ERROR
            }
            else -> CardForm.ERROR
        }
    }
}

/**
 * 匹配牌
 */
fun List<Cards>.contains(car: Car): Boolean = any { it.car == car }

/**
 * 排序,数量多的在前
 */
fun List<Cards>.sort(): List<Cards> = sortedByDescending { it.num }

/**
 * 根据顺序判断手牌数量
 */
fun List<Cards>.inspection(vararg nums: Int): Boolean =
    size == nums.size && indices.all { this[it].num == nums[it] }