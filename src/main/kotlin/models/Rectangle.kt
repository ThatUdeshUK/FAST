package models

import kotlin.math.max
import kotlin.math.min

data class Rectangle(val min: Point, val max: Point) {
    val area: Double
        get() {
            return (max.x - min.x) * (max.y - min.y)
        }

    constructor(minX: Double, minY: Double, maxX: Double, maxY: Double) : this(Point(minX, minY), Point(maxX, maxY))

    fun mapToCell(levelStep: Double): List<Int> {
        return listOf(
            (min.x / levelStep).toInt(),
            (min.y / levelStep).toInt(),
            (max.x / levelStep).toInt(),
            (max.y / levelStep).toInt()
        )
    }

    fun intersection(other: Rectangle): Rectangle {
        return Rectangle(
            Point(max(min.x, other.min.x), max(min.y, other.min.y)),
            Point(min(max.x, max.x), min(max.y, max.y))
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Rectangle

        return min == other.min && max == other.max
    }

    override fun hashCode(): Int {
        var result = min.hashCode()
        result = 31 * result + max.hashCode()
        return result
    }
}