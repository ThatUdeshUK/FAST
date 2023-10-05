package models

class MinimalRangeQuery(
    val id: Int, val keywords: List<String>, val spatialRange: Rectangle, val expireTimestamp: Int
) {
    var deleted: Boolean = false

    override fun toString(): String {
        return "MinimalRangeQuery(queryId=$id, keywords=$keywords, spatialRange=$spatialRange, " +
                "expireTimestamp=$expireTimestamp)"
    }
}