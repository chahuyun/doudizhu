package cn.chahuyun.teafox.game.game

import cn.chahuyun.teafox.game.game.BaghChalGameCore.PosStatue.*


const val rowSize = 5
/**
 * 羊狼棋核心逻辑
 * */
class BaghChalGameCore{
    /**
     * 行数
     * */

    /**
     * 表示落子点位状态的值
     * */
    enum class PosStatue{
        Empty,
        Wolf,
        Sheep
    }
    /**坐标类*/
    class Point(val row: Int, val col: Int){
        fun indexOutOfBounds(): Boolean{
            return row !in 0 until rowSize
        }
    }
    /**
     * 棋盘
     * */
    private val vectorMap:Array<Array<PosStatue>> = Array(rowSize){Array(rowSize){ Empty } }
    /**
     * 羊方玩家剩余落子数*/
    private var piecesLeft:Int = 20
    /**
     * 被吃掉的棋子数*/
    private var piecesCaptured:Int = 0
    /**
     * 游戏顺序
     * false:狼
     * true:羊*/
    private var term:Boolean = false

    init {
        vectorMap[0][0] = Wolf
        vectorMap[0][rowSize-1] = Wolf
        vectorMap[rowSize-1][0] = Wolf
        vectorMap[rowSize-1][rowSize-1] = Wolf
    }
    /**
     * 刷新棋盘
     * */
    fun fresh(){
        for (row in 0 until  rowSize){
            for (col in 0 until  rowSize){
                if ((row == (rowSize-1)) or (col == (rowSize-1))){
                    vectorMap[row][col] = Wolf
                }else { vectorMap[row][col] = Empty }
            }
        }
        vectorMap[0][0] = Wolf
        term = false
        piecesLeft = 20
        piecesCaptured = 0
    }
    /**
     * 逻辑核心，玩家的移动|落子
     * @param sRow 源行坐标
     * @param sCol 源列坐标
     * @param tRow 目标行坐标
     * @param tCol 目标列坐标
     * */
    private fun step(sRow:Int,
                    sCol:Int,
                    tRow:Int,
                    tCol:Int){
        // 1、检查玩家执棋顺序 ---- 似乎无法从这里进行确认，暂时默认执棋顺序是正确的
        // 2、检查玩家移动的合法性
        // 3、执行改变
        if (term){
            sheepStep(sRow,sCol,tRow,tCol)
        }else{
            wolfStep(sRow,sCol,tRow,tCol)
        }
        // 4、交换执棋
        term = !term
        // 5、判断胜负
    }
    /**
     * 狼方执棋*/
    private fun wolfStep(sRow:Int,
                         sCol:Int,
                         tRow:Int,
                         tCol:Int){
        // 1.检查移动合法性
        // ---检查起点和目标点坐标是否越界
        // ---检查移动起点是否为狼棋子
        // ---检查该位置是否被占用
        // ---检查是否能移动到该位置
        if (!(sRow in (0 until rowSize) && sCol in (0 until rowSize))) throw BaghChalGameCoreException("起始坐标错误")
        if (!(tRow in (0 until rowSize) && tCol in (0 until rowSize))) throw BaghChalGameCoreException("目标坐标错误")
        val source = getPosStatue(sRow,sCol)
        if (source != Wolf) throw BaghChalGameCoreException("狼方玩家执棋错误")
        val target = getPosStatue(tRow,tCol)
        if (target != Empty) throw BaghChalGameCoreException("目标位置已有棋子")
        if (sRow == tRow && sCol == tCol) throw BaghChalGameCoreException("起始位置与目标位置相同")
        // 判断移动方向
        val sum = sRow + sCol
        val sPosOnCenter = (sum%2 == 0) // 能否斜移标志位
        when{
            sRow == tRow -> TODO()
            sCol == tCol -> TODO()
            sPosOnCenter -> TODO()
            else -> throw BaghChalGameCoreException("未考虑到的移动情况")
        }

        // 2.执行改变
    }
    /**
     * 羊方执棋*/
    private fun sheepStep(sRow:Int,
                          sCol:Int,
                          tRow:Int,
                          tCol:Int){
        // 1.检查移动合法性
        // 2.执行改变
    }
    /**
     * 检查坐标合法性*/
    private fun checkPos(row:Int,col:Int):Boolean{
        return (row in (0 until rowSize)) && (col in (0 until rowSize))
    }
    /**
     * 检查坐标合法性*/
    private fun checkPos(pos:Point):Boolean{
        return (pos.row in (0 until rowSize)) && (pos.col in (0 until rowSize))
    }
    /**
     * 获取点位值*/
    private fun getPosStatue(row: Int,col: Int):PosStatue{
        return vectorMap[row][col]
    }
    /**
     * 获取点位值*/
    private fun getPosStatue(pos:Point):PosStatue{
        return vectorMap[pos.row][pos.col]
    }
    /**
     * 移动棋子，仅仅只是将棋子移动到目标点位，无视目标点位是否为空，在原点位置为空值*/
    private fun simpleMove(sRow:Int,
                     sCol:Int,
                     tRow:Int,
                     tCol:Int){
        vectorMap[tRow][tCol] = getPosStatue(sRow,sCol)
        vectorMap[sRow][sCol] = Empty
    }
    /**
     * 移动棋子，仅仅只是将棋子移动到目标点位，无视目标点位是否为空，在原点位置为空值*/
    private fun simpleMove(sPos:Point,tPos:Point){
        vectorMap[tPos.row][tPos.col] = getPosStatue(sPos.row,sPos.col)
        vectorMap[sPos.row][sPos.col] = Empty
    }
    /**
     * 落子，仅羊方玩家行为*/
    private fun place(tRow:Int,
                      tCol:Int){
        vectorMap[tRow][tCol] = Sheep
        piecesLeft -= 1
    }
    /**
     * 落子，仅羊方玩家行为*/
    private fun place(pos:Point){
        vectorMap[pos.row][pos.col] = Sheep
        piecesLeft -= 1
    }
    /**
     * 获取一个列表，代表传入点位可以移动的坐标*/
    fun getMoveablePoints(pos:Point):Array<Point>{
        val piece = getPosStatue(pos)
        val close8Point = getClose8PointsFixed(pos) // 在坐标系范围内的(最多)8个点位
        val list = arrayListOf<Point>()

        when(piece){
            Wolf -> {

            }
            Sheep -> TODO()
            Empty -> throw BaghChalGameCoreException("空移动错误")
        }
        return list.toTypedArray()
    }

    /**获取不超过边界的近处八个坐标*/
    private fun getClose8PointsFixed(pos:Point):Array<Point>{
        val list = arrayOf<Point>(
            Point(pos.row-1,pos.col-1),
            Point(pos.row-1,pos.col),
            Point(pos.row-1,pos.col+1),
            Point(pos.row,pos.col-1),
            Point(pos.row,pos.col+1),
            Point(pos.row+1,pos.col-1),
            Point(pos.row+1,pos.col),
            Point(pos.row+1,pos.col+1)
        )
        val result = list.filter{ !it.indexOutOfBounds() }
        return result.toTypedArray()
    }
}

class BaghChalGameCoreException(override val message: String?):Exception(message){}