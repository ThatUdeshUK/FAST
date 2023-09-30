package models

class MinimalRangeQuery(
    val queryId: Int, val keywords: List<String>, val spatialRange: Rectangle, private val expireTimestamp: Int
) {
    override fun toString(): String {
        return "MinimalRangeQuery(queryId=$queryId, keywords=$keywords, spatialRange=$spatialRange, " +
                "expireTimestamp=$expireTimestamp)"
    }
}