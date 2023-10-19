package structures

import models.DataObject
import models.KNNQuery
import models.Query

abstract class TextualNode {
    abstract val queries: MutableList<Query>
    abstract fun isEmpty(): Boolean

    companion object {
        fun searchInQuery(it: Query, obj: DataObject, keywords: List<String>): Query? {
            // TODO - Check expiry
            if (keywords.size >= it.keywords.size && obj.overlaps(it)) {
                if (it is KNNQuery) {
                    it.pushToQueue(obj)
                    if (it.moniteredQueries.contains(obj)) return it
                } else return it
            }
            return null
        }
    }
}

class QueryListNode() : TextualNode() {
    private lateinit var _queries: MutableList<Query>
    override val queries: MutableList<Query>
        get() {
            if (!::_queries.isInitialized) {
                _queries = mutableListOf()
            }
            return _queries
        }

    constructor(query: Query) : this() {
        queries.add(query)
    }

    override fun isEmpty(): Boolean {
        return queries.isEmpty()
    }

    fun find(context: Context, obj: DataObject, keywords: List<String>): List<Query> {
        val results = mutableListOf<Query>()
        queries.forEach { query ->
            context.numberOfObjectSearchInvListNode++
            searchInQuery(query, obj, keywords)?.let { results.add(it) }
        }
        return results
    }
}

class QueryTrieNode : TextualNode() {
    private lateinit var _subtree: HashMap<String, TextualNode>
    private lateinit var _queries: MutableList<Query>
    val subtree: HashMap<String, TextualNode>
        get() {
            if (!::_subtree.isInitialized) {
                _subtree = HashMap() // Type parameters are inferred
            }
            return _subtree
        }
    override val queries: MutableList<Query>
        get() {
            if (!::_queries.isInitialized) {
                _queries = mutableListOf()
            }
            return _queries
        }

    fun find(context: Context, obj: DataObject, keywords: List<String>, start: Int): List<Query> {
        val results = mutableListOf<Query>()
        queries.forEach { query ->
            context.numberOfObjectSearchTrieNode++
            searchInQuery(query, obj, keywords)?.let { results.add(it) }
        }

        if (subtree.isNotEmpty()) {
            keywords.drop(start).forEach { keyword ->
                val textualNode = subtree[keyword] ?: return@forEach

                context.numberOfObjectSearchHashAccess++
                when (textualNode) {
                    is QueryListNode -> {
                        queries.forEach { query ->
                            context.numberOfObjectSearchTrieNode++
                            searchInQuery(query, obj, keywords)?.let { results.add(it) }
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
