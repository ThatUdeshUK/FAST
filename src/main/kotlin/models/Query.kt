package models

abstract class Query(val id: Int, val keywords: List<String>, val et: Int) {
    var deleted: Boolean = false
}

class MinimalRangeQuery(
    id: Int, keywords: List<String>, val spatialRange: Rectangle, et: Int
) : Query(id, keywords, et) {
    override fun toString(): String {
        return "MinimalRangeQuery(queryId=$id, keywords=$keywords, spatialRange=$spatialRange, " +
                "et=$et)"
    }
}

class KNNQuery(
    id: Int, keywords: List<String>, val location: Point, et: Int
) : Query(id, keywords, et) {
    override fun toString(): String {
        return "KNNQuery(queryId=$id, keywords=$keywords, location=$location, et=$et)"
    }
}