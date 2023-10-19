package models

import constants.SpatioTextualConst
import java.util.Comparator
import java.util.PriorityQueue
import kotlin.math.pow

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
    id: Int, keywords: List<String>, val location: Point, val K: Int, et: Int
) : Query(id, keywords, et) {
    val ar: Double = SpatioTextualConst.MAX_RANGE_X

    private lateinit var _moniteredQueries: PriorityQueue<DataObject>
    val moniteredQueries: PriorityQueue<DataObject>
        get() {
            if (!::_moniteredQueries.isInitialized) {
                _moniteredQueries = PriorityQueue(K, EuclidianCompartor(location))
            }
            return _moniteredQueries
        }

    fun pushToQueue(obj: DataObject) {
        if (!moniteredQueries.contains(obj)) {
            moniteredQueries.add(obj)
            if (moniteredQueries.size > K) {
                moniteredQueries.poll()
            }
        }
    }

    override fun toString(): String {
        return "KNNQuery(queryId=$id, keywords=$keywords, location=$location, AR: $ar, et=$et)"
    }
}

internal class EuclidianCompartor(private var point: Point) : Comparator<DataObject> {
    override fun compare(e1: DataObject, e2: DataObject): Int {
        val val1 = (point.x - e1.location.x).pow(2) + (point.y - e1.location.y).pow(2)
        val val2 = (point.x - e2.location.x).pow(2) + (point.y - e2.location.y).pow(2)

        return if (val1 < val2) 1 else if (val1 == val2) 0 else -1
    }
}