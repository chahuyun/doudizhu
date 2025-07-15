package cn.chahuyun.teafox.game.game

import cn.chahuyun.teafox.game.GameEvent
import cn.chahuyun.teafox.game.GameType
import cn.chahuyun.teafox.game.Player
import cn.chahuyun.teafox.game.game.BaghChalGameCore.MoveDirection.*
import cn.chahuyun.teafox.game.game.BaghChalGameCore.PosStatue.*
import net.mamoe.mirai.contact.Group

/**
 * 行数
 * */
const val rowSize = 5

/**
 * 羊狼棋核心逻辑
 * */
class BaghChalGameCore {
    /**
     * 表示落子点位状态的值
     * */
    enum class PosStatue {
        Empty,
        Wolf,
        Sheep
    }

    /** 坐标类 */
    class Point(val row: Int, val col: Int) {
        /**
         * 判断是否超出坐标边界，true：超出，false：未超出
         * */
        fun outOfBounds(): Boolean {
            return (row !in 0 until rowSize) || (col !in 0 until rowSize)
        }

        /** 是否可以斜角移动*/
        fun isDiagonal(): Boolean {
            return (row + col) % 2 == 0
        }

        fun up():Point{ return Point(row-1, col) }
        fun down():Point{return Point(row+1, col)}
        fun left():Point{return Point(row, col-1)}
        fun right():Point{return Point(row, col+1)}
        fun upLeft():Point{return Point(row-1, col-1)}
        fun upRight():Point{return Point(row-1, col+1)}
        fun downLeft():Point{return Point(row+1, col-1)}
        fun downRight():Point{return Point(row+1, col+1)}
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Point

            if (row != other.row) return false
            if (col != other.col) return false

            return true
        }

        override fun hashCode(): Int {
            var result = row
            result = 31 * result + col
            return result
        }


    }
    var winner:PosStatue = Empty
    /** 棋盘 */
    private val vectorMap: Array<Array<PosStatue>> = Array(rowSize) { Array(rowSize) { Empty } }

    /** 羊方玩家剩余落子数 */
    private var piecesLeft: Int = 20

    /** 被吃掉的棋子数*/
    private var piecesCaptured: Int = 0

    /**
     * 游戏顺序
     * false:狼
     * true:羊*/
    private var term: Boolean = false


    /**
     * 初始化*/
    init {
        vectorMap[0][0] = Wolf
        vectorMap[0][rowSize - 1] = Wolf
        vectorMap[rowSize - 1][0] = Wolf
        vectorMap[rowSize - 1][rowSize - 1] = Wolf
    }

    /**
     * 刷新棋盘
     * */
    fun fresh() {
        for (row in 0 until rowSize) {
            for (col in 0 until rowSize) {
                if ((row == (rowSize - 1)) || (col == (rowSize - 1))) {
                    vectorMap[row][col] = Wolf
                } else {
                    vectorMap[row][col] = Empty
                }
            }
        }
        vectorMap[0][0] = Wolf
        term = false
        piecesLeft = 20
        piecesCaptured = 0
        winner = Empty
    }

    /**
     * 逻辑核心，玩家的移动|落子
     * @param sPos 起始坐标
     * @param tPos 目标坐标
     * */
    fun step(sPos: Point, tPos: Point) {
        if (term) {
            sheepStep(sPos, tPos)
        } else {
            wolfStep(sPos, tPos)
        }
        // 判断胜负
        if (!term && piecesCaptured >= 5){
            // 狼获胜
            winner = Wolf
            return
        }
        if (term && !hasWay2Go()){
            // 羊获胜
            winner = Sheep
            return
        }

        // 交换执棋顺序
        term = !term
    }
    /**判断狼方没有路可以走*/
    private fun hasWay2Go():Boolean{
        // 获取每个狼棋子的坐标
        val wolfs = arrayListOf<Point>()
        outer@for ((rowIndex,row) in vectorMap.withIndex()){
            for ((colIndex,unit) in row.withIndex()){
                if (unit == Wolf){
                    wolfs.add(Point(rowIndex, colIndex))
                }
                if (wolfs.size>=4){
                    break@outer
                }
            }
        }

        for (wolf in wolfs){
            // 判断每个狼棋子是否有路可以走
            for(p in getPoints(wolf)){
                if (getPosStatue(p)==Empty){
                    return true
                }
            }
            // 上下左右
            if(eatable(wolf,Up) || eatable(wolf,Right) || eatable(wolf,Down) || eatable(wolf,Left)) return true
            if (wolf.isDiagonal()){
                if(eatable(wolf, UpRight) || eatable(wolf, UpLeft) || eatable(wolf, DownRight)|| eatable(wolf, DownLeft)){
                    return true
                }
            }
        }
        return false
    }

    enum class MoveDirection {
        Up,Right,Down,Left,UpRight,DownRight,UpLeft,DownLeft
    }

    /**判断狼在某个方向是否能吃子*/
    private fun eatable(wolf:Point,dir: MoveDirection):Boolean{
        when(dir){
            Up -> {
                val up = wolf.up()
                val upper = up.up()
                if (up.outOfBounds()||upper.outOfBounds()) return false
                return (getPosStatue(up)==Sheep&&getPosStatue(upper)==Empty)
            }
            Right -> {
                val right = wolf.right()
                val righter = right.right()
                if (right.outOfBounds()||righter.outOfBounds()) return false
                return (getPosStatue(right)==Sheep&&getPosStatue(righter)==Empty)
            }
            Down -> {
                val down = wolf.down()
                val downer = down.down()
                if (down.outOfBounds()||downer.outOfBounds()) return false
                return (getPosStatue(down)==Sheep&&getPosStatue(downer)==Empty)
            }
            Left -> {
                val left = wolf.left()
                val lefter = left.left()
                if (left.outOfBounds()||lefter.outOfBounds()) return false
                return (getPosStatue(left)==Sheep&&getPosStatue(lefter)==Empty)
            }
            UpRight -> {
                val upRight = wolf.upRight()
                val downer = upRight.upRight()
                if (upRight.outOfBounds()||downer.outOfBounds()) return false
                return (getPosStatue(upRight)==Sheep&&getPosStatue(downer)==Empty)
            }
            DownRight -> {
                val upRight = wolf.downRight()
                val downer = upRight.downRight()
                if (upRight.outOfBounds()||downer.outOfBounds()) return false
                return (getPosStatue(upRight)==Sheep&&getPosStatue(downer)==Empty)
            }
            UpLeft -> {
                val upRight = wolf.upLeft()
                val downer = upRight.upLeft()
                if (upRight.outOfBounds()||downer.outOfBounds()) return false
                return (getPosStatue(upRight)==Sheep&&getPosStatue(downer)==Empty)
            }
            DownLeft -> {
                val upRight = wolf.downLeft()
                val downer = upRight.downLeft()
                if (upRight.outOfBounds()||downer.outOfBounds()) return false
                return (getPosStatue(upRight)==Sheep&&getPosStatue(downer)==Empty)
            }
        }
    }

    /**
     * 狼方执棋
     * */
    private fun wolfStep(sp:Point,tp:Point) {
        // 1.检查移动合法性
        // ---检查起点和目标点坐标是否越界
        // ---检查移动起点是否为狼棋子
        // ---获取起始棋子所有可移动点位
        // ---对比目标点位是否包含在可移动点位中
        if (sp.outOfBounds()) {
            throw BaghChalGameCoreException("起始坐标越界")
        }
        if (tp.outOfBounds()) throw BaghChalGameCoreException("目标坐标越界")
        val source = getPosStatue(sp)
        if (source != Wolf) throw BaghChalGameCoreException("狼方玩家执棋错误")
        val target = getPosStatue(tp)
        if (target != Empty) throw BaghChalGameCoreException("目标位置已有棋子")
        // 2.执行改变
        when(tp){
            in getPoints(sp)->{
                //进行移动
                simpleMove(sp,tp)
            }
            in getPoints(sp,2)->{
                //吃子
                val midPoint=getMidPoint(sp,tp)
                if (getPosStatue(midPoint)==Sheep){
                    simpleMove(sp,tp)
                    remove(midPoint)
                }else{ throw BaghChalGameCoreException("间隔点没有棋子|不可吃") }
            }
            else -> {
                //落子点不在可移动范围内
                throw BaghChalGameCoreException("不在可移动范围内")
            }
        }
    }

    /**
     * 羊方执棋
     * */
    private fun sheepStep(sp:Point,tp:Point) {
        // 1.检查移动合法性
        // ---检查起点和目标点坐标是否越界
        // ---检查移动起点是否为羊棋子
        // ---获取起始棋子所有可移动点位
        // ---对比目标点位是否包含在可移动点位中
        if (piecesLeft>0){
            //不检查起点，只看终点是否可以落子
            if (tp.outOfBounds()) throw BaghChalGameCoreException("落子越界")
            if (getPosStatue(tp) != Empty) throw BaghChalGameCoreException("落子位置已经占用")
            //执行
            place(tp)
        }else{
            if (sp.outOfBounds() || tp.outOfBounds()) {throw BaghChalGameCoreException("起点|终点越界")}
            if (getPosStatue(sp)!=Sheep) throw BaghChalGameCoreException("玩家移动棋子不是羊棋")
            if (getPosStatue(tp)!= Empty) throw BaghChalGameCoreException("移动目标位置被占用")
            val moveable = getPoints(sp)
            if (tp !in moveable) throw BaghChalGameCoreException("目标位置不在可移动范围")
            //执行
            simpleMove(sp,tp)
        }
    }

    /**删除被吃掉的棋子，因为只有羊棋会被吃，顺便对吃子进行计数*/
    private fun remove(p:Point){
        vectorMap[p.row][p.col] = Empty
        piecesCaptured += 1
    }

    /**
     * 获取点位值*/
    private fun getPosStatue(pos: Point): PosStatue {
        return vectorMap[pos.row][pos.col]
    }

    /**
     * 移动棋子，仅仅只是将棋子移动到目标点位，无视目标点位是否为空，在原点位置为空值*/
    private fun simpleMove(sPos: Point, tPos: Point) {
        vectorMap[tPos.row][tPos.col] = getPosStatue(sPos)
        vectorMap[sPos.row][sPos.col] = Empty
    }

    /** 落子，仅羊方玩家行为*/
    private fun place(pos: Point) {
        vectorMap[pos.row][pos.col] = Sheep
        piecesLeft -= 1
    }

    /** 获取不超过边界的八个坐标*/
    private fun get8Points(pos: Point, stepLength: Int = 1): Array<Point> {
        val list = arrayOf(
            Point(pos.row - stepLength, pos.col - stepLength),
            Point(pos.row - stepLength, pos.
            col),
            Point(pos.row - stepLength, pos.col + stepLength),
            Point(pos.row, pos.col - stepLength),
            Point(pos.row, pos.col + stepLength),
            Point(pos.row + stepLength, pos.col - stepLength),
            Point(pos.row + stepLength, pos.col),
            Point(pos.row + stepLength, pos.col + stepLength)
        )
        val result = list.filter { !it.outOfBounds() }
        return result.toTypedArray()
    }

    /** 获取不超过边界的四个坐标*/
    private fun get4Points(pos: Point, stepLength: Int = 1): Array<Point> {
        val list = arrayOf(
            Point(pos.row - stepLength, pos.col),
            Point(pos.row, pos.col - stepLength),
            Point(pos.row, pos.col + stepLength),
            Point(pos.row + stepLength, pos.col),
        )
        val result = list.filter { !it.outOfBounds() }
        return result.toTypedArray()
    }

    /** 获取直线步长坐标 */
    private fun getPoints(pos: Point, stepLength: Int = 1): Array<Point> {
        return if (pos.isDiagonal()){
            get8Points(pos, stepLength)
        }else{
            get4Points(pos, stepLength)
        }
    }

    /** 获取两个点之间的夹点，使用时保证两个点步长为 2 */
    private fun getMidPoint(pos1: Point, pos2: Point):Point{
        return Point((pos1.row + pos2.row)/2,(pos1.col + pos2.col)/2)
    }

    fun show() {
        val sheep = "⛊"
        val wolf = "⛉"
        val empty = "⛌"
        println(
            """
            --------------------------
            sheep:${sheep} wolf:${wolf} empty:${empty}
            当前${if(term) sheep else wolf}
        """.trimIndent()
        )
        println(" 1 2 3 4 5")
        for (row in vectorMap.indices) {
            print(row+1)
            for (piece in vectorMap[row]) {
                when (piece) {
                    Empty -> print(empty)
                    Wolf -> print(wolf)
                    Sheep -> print(sheep)
                }
            }
            println()
        }
    }
}

class BaghChalGameCoreException(override val message: String?) : Exception(message)

class BaghChalGame(override val group: Group,
                   override val players: List<Player>,
                   override val gameType: GameType) :GameTable{
    /**
     * ->游戏开始
     * ->检查好友，开启禁言，发牌
     * ->进入轮询消息监听，开始对局
     */

    override suspend fun start() {
        group.sendMessage("TODO:还没做好，敬请期待\n欸嘿？（歪头）")
        GameEvent.cancelGame(group)
    }

    /**
     * ->游戏初期阶段
     * ->抢地主阶段
     * ->决定地主，补牌
     */
    override suspend fun initial() {
        TODO("Not yet implemented")
    }

    /**
     * ->游戏对局阶段
     * ->出牌
     */
    override suspend fun cards() {
        TODO("Not yet implemented")
    }

    /**
     * ->对局结束
     * ->计算倍率，计算积分，操作结果
     */
    override suspend fun stop() {
        TODO("Not yet implemented")
    }

}
