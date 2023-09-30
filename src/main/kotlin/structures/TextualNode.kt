package structures

import models.MinimalRangeQuery

abstract class TextualNode {
    abstract val queries: MutableList<MinimalRangeQuery>
    abstract fun isEmpty(): Boolean
}

class QueryListNode(): TextualNode() {
    private lateinit var _queries: MutableList<MinimalRangeQuery>
    override val queries: MutableList<MinimalRangeQuery>
        get() {
            if (!::_queries.isInitialized) {
                _queries = mutableListOf() // Type parameters are inferred
            }
            return _queries
        }

    constructor(query: MinimalRangeQuery): this() {
        queries.add(query)
    }

    override fun isEmpty(): Boolean {
        return queries.isEmpty()
    }
}

class QueryTrieNode: TextualNode() {
    private lateinit var _subtree: HashMap<String, TextualNode>
    private lateinit var _queries: MutableList<MinimalRangeQuery>
    val subtree: HashMap<String, TextualNode>
        get() {
            if (!::_subtree.isInitialized) {
                _subtree = HashMap() // Type parameters are inferred
            }
            return _subtree
        }
    override val queries: MutableList<MinimalRangeQuery>
        get() {
            if (!::_queries.isInitialized) {
                _queries = mutableListOf()
            }
            return _queries
        }

    override fun isEmpty(): Boolean {
        return subtree.isEmpty() && queries.isEmpty()
    }
}
