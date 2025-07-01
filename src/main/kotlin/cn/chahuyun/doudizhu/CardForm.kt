package cn.chahuyun.doudizhu

enum class CardForm {
    /**
     * 单牌
     */
    SINGLE,

    /**
     * 对子
     */
    PAIR,

    /**
     * 三张
     */
    TRIPLE,

    /**
     * 三带一
     */
    TRIPLE_ONE,

    /**
     * 三带一对
     */
    TRIPLE_TWO,

    /**
     * 顺子
     */
    QUEUE,

    /**
     * 连对
     */
    QUEUE_TWO,

    /**
     * 炸弹
     */
    BOMB,

    /**
     * 王炸
     */
    GHOST_BOMB,

    /**
     * 错误
     */
    ERROR
}

object CardFormUtil {
    /**
     * 匹配牌型
     */
    fun match(cards: List<Cards>): CardForm {
        return when {
            cards.size == 1 -> CardForm.SINGLE
            cards.size == 2 -> when {
                cards[0].car == cards[1].car -> CardForm.PAIR
                cards.contains(Car.SMALL_JOKER) && cards.contains(Car.BIG_JOKER) -> CardForm.GHOST_BOMB
                else -> CardForm.ERROR
            }

            cards.size == 2 && cards[0].car == cards[1].car -> CardForm.PAIR
            cards.size == 3 -> {
                when {
                    cards[0].car == cards[1].car && cards[1].car == cards[2].car -> CardForm.TRIPLE
                    cards[0].car == cards[1].car || cards[1].car == cards[2].car -> CardForm.TRIPLE_ONE
                    else -> CardForm.TRIPLE_TWO
                }
            }

        }
    }

}