package models

import exceptions.InvalidState
import structures.SpatialCell
import kotlin.math.pow
import kotlin.math.sqrt

class DataObject(
    private val id: Int,
    val keywords: List<String>,
    val location: Point,
    val st: Int,
    val et: Int
) {

    constructor(id: Int, keywords: List<String>, location: Point, st: Int): this(id, keywords, location, st, st)

    fun overlaps(query: Query): Boolean {
        return when(query) {
            is MinimalRangeQuery -> overlapsSpatially(query.spatialRange) && containsTextually(query.keywords)
            is KNNQuery -> overlapsSpatially(query.location, query.ar) && containsTextually(query.keywords)
            else -> throw InvalidState("Should not arrive here.")
        }
    }

    private fun containsTextually(other: List<String>): Boolean {
        if (keywords.size < other.size) return false
        var i = 0
        var j = 0
        while (i < keywords.size && j < other.size) {
            val `val` = keywords[i].compareTo(other[j], ignoreCase = true)
            if (`val` < 0) { //str1 is greater than str2
                i++
            } else if (`val` > 0) { //str2 is greater than str1
                return false // a String is not matched
            } else {
                i++
                j++
            }
        }
        return j == other.size
    }

    private fun overlapsSpatially(rectangle: Rectangle): Boolean {
        return !(location.x - rectangle.min.x < 0 || location.x - rectangle.max.x > 0 ||
                location.y - rectangle.min.y < 0 || location.y - rectangle.max.y > 0)
    }

    private fun overlapsSpatially(loc: Point, ar: Double): Boolean {
        return sqrt((location.x - loc.x).pow(2.0) + (location.y - loc.y).pow(2.0)) < ar
    }

    fun calcCoordinate(i: Int, step: Double, granI: Int): Int {
        val xCell: Int = (location.x / step).toInt()
        val yCell: Int = (location.y / step).toInt()
        return SpatialCell.calcCoordinate(i, xCell, yCell, granI)
    }

    override fun toString(): String {
        return "DataObject(id=$id, keywords=$keywords, location=$location, st=$st, et=$et)"
    }
}