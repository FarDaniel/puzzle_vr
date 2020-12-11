package hu.szakdolgozat.puzzlevr.puzzle

import android.util.Log
import kotlin.math.atan2

class Position(var x: Float = 0.0f, var y: Float = 0.0f, var z: Float = 0.0f) {
    enum class Direction {
        UP, DOWN, LEFT, RIGHT, IN, OUT
    }

    constructor(position: FloatArray) : this(position[0], position[1], position[2])

    companion object {
        fun getDirectionVector(direction: Direction, distance: Float): Position {
            return when (direction) {
                Direction.UP -> Position(0f, 0f, distance)
                Direction.DOWN -> Position(0f, 0f, -distance)
                Direction.LEFT -> Position(-distance, 0f, 0f)
                Direction.RIGHT -> Position(distance, 0f, 0f)
                Direction.IN -> Position(0f, distance, 0f)
                Direction.OUT -> Position(0f, -distance, 0f)
            }
        }
    }

    //Are the two position near eachother on a surface
    fun isNear(position: Position, distance: Float = 0.5f): Boolean {
        return (kotlin.math.abs(this.x - position.x) < distance && kotlin.math.abs(this.z - position.z) < distance)
    }

    fun reverseOnSurface(): Position {
        return Position(-x, y, -z)
    }

    operator fun plus(pos: Position): Position {
        return Position(pos.x + x, pos.y + y, pos.z + z)
    }

    operator fun minus(pos: Position): Position {
        return Position(pos.x - x, pos.y - y, pos.z - z)
    }

    //Two dimensional addition
    fun addOnSurface(pos: Position): Position {
        return Position(this.x + pos.x, this.y, this.z + pos.z)
    }

    //Get the angle between the position and a point on a surface
    fun getAngleWithPoint(x: Float, z: Float): Float {
        return -Math.toDegrees(atan2((this.x - x).toDouble(), (this.z - z).toDouble())).toFloat()
    }

    fun writeOut(pre: String) {
        Log.d("Position", "$pre\n x:$x\n y:$y\n z:$z")
    }

}