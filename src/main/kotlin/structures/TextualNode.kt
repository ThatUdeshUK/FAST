package structures

import models.DataObject
import models.MinimalRangeQuery

abstract class TextualNode {
    abstract val queries: MutableList<MinimalRangeQuery>
    abstract fun isEmpty(): Boolean
}

class QueryListNode() : TextualNode() {
    private lateinit var _queries: MutableList<MinimalRangeQuery>
    override val queries: MutableList<MinimalRangeQuery>
        get() {
            if (!::_queries.isInitialized) {
                _queries = mutableListOf() // Type parameters are inferred
            }
            return _queries
        }

    fun find(context: Context, obj: DataObject, keywords: List<String>): List<MinimalRangeQuery> {
        val results = mutableListOf<MinimalRangeQuery>()
        _queries.forEach {
            context.numberOfObjectSearchInvListNode++
            if (keywords.size >= it.keywords.size && obj.overlaps(it)) {
                results.add(it)
            }
        }
        return results
    }

    constructor(query: MinimalRangeQuery) : this() {
        queries.add(query)
    }

    override fun isEmpty(): Boolean {
        return queries.isEmpty()
    }
}

class QueryTrieNode : TextualNode() {
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

    fun find(context: Context, obj: DataObject, keywords: List<String>, start: Int): List<MinimalRangeQuery> {
        val results = mutableListOf<MinimalRangeQuery>()

        queries.forEach {
            context.numberOfObjectSearchTrieNode++
            if (obj.overlapsSpatially(it.spatialRange)) {
                results.add(it)
            }
        }

        if (subtree.isNotEmpty()) {
            keywords.drop(start).forEach { keyword ->
                val textualNode = subtree[keyword] ?: return@forEach

                context.numberOfObjectSearchHashAccess++
                when (textualNode) {
                    is QueryListNode -> {
                        _queries.forEach {
                            context.numberOfObjectSearchTrieNode++
                            if (obj.overlaps(it)) results.add(it)
                        }
                    }

                    is QueryTrieNode -> {
                        results.addAll(textualNode.find(context, obj, keywords, start + 1))
                    }
                }
            }
        }

        return results
    }

    override fun isEmpty(): Boolean {
        return subtree.isEmpty() && queries.isEmpty()
    }
}
