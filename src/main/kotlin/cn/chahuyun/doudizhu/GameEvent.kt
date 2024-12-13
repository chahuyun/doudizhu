package cn.chahuyun.doudizhu

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize

/**
 * 游戏事件
 *
 * @author Moyuyanli
 * @date 2024-12-13 11:09
 */
@EventComponent
class GameEvent {

    @MessageAuthorize(text = ["开桌"])
    fun startGame() {

    }
}
