package models

import structures.SpatialCell

class DataObject(private val id: Int, val keywords: List<String>, private val location: Point, private val timestamp: Int) {

    fun overlaps(query: MinimalRangeQuery): Boolean {
        return overlapsSpatially(query.spatialRange) && containsTextually(query.keywords)
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

    fun overlapsSpatially(rectangle: Rectangle): Boolean {
        return !(location.x - rectangle.min.x < 0 || location.x - rectangle.max.x > 0 ||
                location.y - rectangle.min.y < 0 || location.y - rectangle.max.y > 0)
    }

    fun calcCoordinate(i: Int, step: Double, granI: Int): Int {
        val xCell: Int = (location.x / step).toInt()
        val yCell: Int = (location.y / step).toInt()
        return SpatialCell.calcCoordinate(i, xCell, yCell, granI)
    }

    override fun toString(): String {
        return "DataObject(id=$id, keywords=$keywords, location=$location, timestamp=$timestamp)"
    }
}