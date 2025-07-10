package cn.chahuyun.teafox.game.util

import cn.chahuyun.teafox.game.Car
import cn.chahuyun.teafox.game.Car.*
import cn.chahuyun.teafox.game.Cards

/**
 * 游戏牌工具
 */
object CardUtil {

    // == 打印 ==

    /**
     * 打印牌
     */
    @JvmName("showCar")
    fun List<Car>.show(): String {
        return sort().joinToString { "$it" }
    }

    /**
     * 打印牌
     */
    @JvmName("showCars")
    fun List<Cards>.show(): String {
        return sort().flatMap { cards -> List(cards.num) { "${cards.car}" } }.joinToString()
    }

    /**
     * 反方向打印牌
     */
    @JvmName("cardsShow")
    fun List<Cards>.cardsShow(): String {
        return sort().reversed().flatMap { cards -> List(cards.num) { "${cards.car}" } }.joinToString()
    }

    // == 转换 ==

    /**
     * 将List<Cards>转换为List<Car>
     */
    @JvmName("toListCar")
    fun List<Cards>.toListCar(): List<Car> {
        return flatMap { cards -> List(cards.num) { cards.car } }
    }

    /**
     * 将List<Car>转换为List<Cards>
     */
    @JvmName("toListCards")
    fun List<Car>.toListCards(): List<Cards> {
        return this.groupingBy { it }
            .eachCount()
            .map { (car, count) -> Cards(car, count) }
    }

    // == 排序 ==

    /**
     * 按照斗地主的牌的大小进行排序
     */
    @JvmName("sortCar")
    fun List<Car>.sort(): List<Car> {
        return sortedByDescending { it.sort }
    }

    /**
     * 按照斗地主的牌的大小进行排序
     */
    @JvmName("sortCards")
    fun List<Cards>.sort(): List<Cards> {
        return sortedByDescending { it.car.sort }
    }

    /**
     * 排序,数量多的在前
     */
    fun List<Cards>.sortNum(): List<Cards> = sortedByDescending { it.num }

    // == 其他工具 ==

    /**
     * 匹配牌
     */
    fun List<Cards>.contains(car: Car): Boolean = any { it.car == car }


    /**
     * 判断是否连续（允许乱序）
     * 排除2大小王
     */
    @JvmName("continuousCards")
    fun continuous(cars: List<Cards>): Boolean {
        return continuous(cars.map { it.car })
    }

    /**
     * 判断是否连续（允许乱序）
     * 排除2大小王
     */
    @JvmName("continuousCar")
    fun continuous(cars: List<Car>): Boolean {
        if (cars.size <= 1) return false

        //链子中不能有大小王和2
        if (cars.contains(SMALL_JOKER) || cars.contains(BIG_JOKER) || cars.contains(TWO)) {
            return false
        }

        // 先根据 value 排序
        val sorted = cars.sortedBy { it.value }

        // 再检查是否连续
        return sorted.zipWithNext().all { (car1, car2) ->
            car2.value == car1.value + 1
        }
    }
}