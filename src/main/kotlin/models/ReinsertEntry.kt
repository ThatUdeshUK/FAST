package models

class ReinsertEntry(val range: Rectangle, val query: MinimalRangeQuery) {
    constructor(query: MinimalRangeQuery): this(query.spatialRange, query)

    override fun toString(): String {
        return "ReinsertEntry(range=$range, query=$query)"
    }


}
