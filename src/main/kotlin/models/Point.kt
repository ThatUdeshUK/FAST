package models

class Point(var x: Double = 0.0, var y: Double = 0.0) {

    fun toRect(r: Int): Rectangle {
        return Rectangle(x - r, y - r, x + r, y + r)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Point

        return x == other.x && y == other.y
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        return result
    }

    override fun toString(): String {
        return "Point(x=$x, y=$y)"
    }


}