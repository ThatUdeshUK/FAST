package structures

import constants.SpatioTextualConst
import exceptions.InvalidState
import models.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

class SpatialCell(
    private val context: Context,
    private val bounds: Rectangle,
    private val _coordinatePoint: Point,
    private val _level: Int
) {
    private lateinit var _textualIndex: ConcurrentHashMap<String, TextualNode>
    val textualIndex: ConcurrentHashMap<String, TextualNode>?
        get() {
            if (!::_textualIndex.isInitialized) return null
            return _textualIndex
        }
    val coordinatePoint: Point
        get() = _coordinatePoint
    val level: Int
        get() = _level

    init {
        bounds.max.x -= 0.001
        bounds.max.y -= 0.001
    }

    fun addQuery(keyword: String, query: Query, swap: Boolean = false): Set<ReinsertEntry> {
        if (!::_textualIndex.isInitialized) {
            _textualIndex = ConcurrentHashMap()
        }

        val queue: Queue<Query> = LinkedList()
        val nextLevelQueries: MutableSet<ReinsertEntry> = mutableSetOf()
        if (insertAtKeyword(keyword, query)) return nextLevelQueries else queue.add(query)

        while (queue.isNotEmpty()) {
            val nextQuery = queue.remove()
            if (swap && swapToInfrequent(keyword, nextQuery)) continue

            val (altKeyword, _) = getAlternateKeyword(nextQuery)
            if (keyword != altKeyword && !altKeyword.isNullOrEmpty() && insertAtKeyword(altKeyword, nextQuery)) continue
            else {
                // Can't find infrequent keyword. Convert all keywords to tries.
                nextQuery.keywords.forEach {
                    val root = _textualIndex[it]
                    if (root is QueryListNode) {
                        queue.addAll(root.queries)
                        _textualIndex[it] = QueryTrieNode()
                        context.numberOfTrieNodes++
                    }
                }
            }

            var currentNode = _textualIndex[nextQuery.keywords[0]] as QueryTrieNode
            var inserted = false
            nextQuery.keywords.drop(1).forEachIndexed { i, newKeyword ->
                if (inserted) return@forEachIndexed
                val index = i + 1
                val node = currentNode.subtree[newKeyword]

                if (node == null) {
                    currentNode.subtree[newKeyword] = QueryListNode(nextQuery)
                    inserted = true
                } else if (node is QueryListNode && node.queries.size < context.trieSplitThreshold) { // This is leq in impl
                    checkExpiryAndInsert(node, nextQuery)
                    inserted = true
                } else if (node is QueryListNode && node.queries.size >= context.trieSplitThreshold) {
                    val newTrie = QueryTrieNode()
                    context.numberOfTrieNodes++

                    node.queries.add(nextQuery)
                    currentNode.subtree[newKeyword] = newTrie

                    node.queries.forEach { otherQuery ->
                        if (otherQuery.et > context.queryTimeStampCounter) {
                            if (otherQuery.keywords.size > index + 1) {
                                val otherKeyword = otherQuery.keywords[index + 1]
                                val otherCell = currentNode.subtree[otherKeyword] ?: QueryListNode()
                                if (!otherCell.queries.contains(otherQuery)) {
                                    otherCell.queries.add(otherQuery)
                                }
                                newTrie.subtree[otherKeyword] = otherCell
                            } else {
                                newTrie.queries.add(otherQuery)
                            }
                        } else {
                            removeQuery(otherQuery)
                        }
                    }
                    inserted = true
                } else if (node is QueryTrieNode) {
                    if (index < nextQuery.keywords.size - 1) {
                        currentNode = node
                    } else {
                        node.queries.add(nextQuery)
                    }
                    inserted = true
                    if (node.queries.filterIsInstance<MinimalRangeQuery>().size > context.degredationRatio) {
                        nextLevelQueries.addAll(findQueriesToReinsert(node))
                    }
                }
            }

            if (!inserted) {
                currentNode.queries.add(nextQuery)
            }
        }

        return nextLevelQueries
    }

    private fun insertAtKeyword(keyword: String, query: Query): Boolean {
        val root = _textualIndex[keyword]

        if (root == null) {
            // Keyword doesn't exist. Add a new top-level node with keyword: query.
            context.numberOfHashEntries++
            context.numberOfInsertedTextualNodes++
            _textualIndex[keyword] = QueryListNode(query)
            return true
        } else {
            // Keyword already exists
            return if (root is QueryListNode && root.queries.size < context.trieSplitThreshold) {
                checkExpiryAndInsert(root, query)
                true
            } else if (root is QueryListNode && root.queries.size >= context.trieSplitThreshold) {
                root.queries.contains(query)
            } else if (root is QueryTrieNode) false else {
                throw InvalidState("Keyword insertion should not arrive here!")
            }
        }
    }

    private fun checkExpiryAndInsert(node: TextualNode, query: Query) {
        var inserted = false
        node.queries.forEach {
            if (it == query) inserted = true
            if (it.et <= context.queryTimeStampCounter) {
                removeQuery(it)
            }
        }
        if (!inserted) {
            node.queries.add(query)
            context.numberOfInsertedTextualNodes++
        }
    }

    private fun findQueriesToReinsert(node: QueryTrieNode): List<ReinsertEntry> {
        val compartor = SpatialOverlapCompartor(bounds)
        val mbrQueries = node.queries.filterIsInstance<MinimalRangeQuery>().sortedWith(compartor)
        val nextLevelQueries = mbrQueries.takeLast(mbrQueries.size / 2)
        node.queries.removeAll(nextLevelQueries)
        return nextLevelQueries.map { ReinsertEntry(bounds.intersection(it.spatialRange), it) }
    }

    private fun removeQuery(query: Query) {
        if (!query.deleted) {
            query.deleted = true
            for (keyword in query.keywords) {
                context.keywordFrequencyMap[keyword]?.let { it.queryCount-- }
            }
        }
    }

    private fun getAlternateKeyword(query: Query): Pair<String?, Int> {
        var minSize = Int.MAX_VALUE
        var minKeyword: String? = null

        query.keywords.forEach {
            val root = _textualIndex[it]

            if (root == null) {
                minSize = 0
                minKeyword = it
            } else if (root is QueryListNode) {
                val size = root.queries.size
                if (size < minSize) {
                    minSize = size
                    minKeyword = it
                }
            }
        }

        return Pair(minKeyword, minSize)
    }

    fun searchQueries(obj: DataObject, keywords: List<String>): Pair<List<String>, List<Query>> {
        val results = mutableListOf<Query>()
        val pendingKeywords = mutableListOf<String>()
        keywords.forEach { keyword ->
            val textualNode = _textualIndex[keyword]
            context.numberOfObjectSearchHashAccess++
            when (textualNode) {
                is QueryListNode -> {
                    val found = textualNode.find(context, obj, keywords)
                    results.addAll(found)
                }

                is QueryTrieNode -> {
                    pendingKeywords.add(keyword)
                }
            }
        }
        pendingKeywords.forEachIndexed { index, keyword ->
            val textualNode = _textualIndex[keyword] as QueryTrieNode
            context.numberOfObjectSearchTrieNodeAccess++
            results.addAll(textualNode.find(context, obj, keywords, index + 1))
        }
        return Pair(pendingKeywords, results)
    }

    fun overlapsSpatially(other: Rectangle): Boolean {
        return ((bounds.min.x <= other.max.x || abs(bounds.min.x - other.max.x) < .000001) &&
                (bounds.max.x >= other.min.x || abs(bounds.max.x - other.min.x) < .000001) &&
                (bounds.min.y <= other.max.y || abs(bounds.min.y - other.max.y) < .000001) &&
                (bounds.max.y >= other.min.y || abs(bounds.max.y - other.min.y) < .000001))
    }

    fun overlapsSpatially(other: Point): Boolean {
        return ((bounds.min.x <= other.x || abs(bounds.min.x - other.x) < .000001) &&
                (bounds.max.x >= other.x || abs(bounds.max.x - other.x) < .000001) &&
                (bounds.min.y <= other.y || abs(bounds.min.y - other.y) < .000001) &&
                (bounds.max.y >= other.y || abs(bounds.max.y - other.y) < .000001))
    }

    private fun swapToInfrequent(keyword: String, query: Query): Boolean {
        val frequentRoot = _textualIndex[keyword]

        if (frequentRoot is QueryListNode) {
            var minSize = Int.MAX_VALUE
            var swappableKeyQuery: Pair<String?, Query>? = null
            frequentRoot.queries.forEach {
                val (altKeyword, currentSize) = getAlternateKeyword(it)
                if (currentSize < minSize && currentSize < SpatioTextualConst.TRIE_SPLIT_THRESHOLD) {
                    minSize = currentSize
                    swappableKeyQuery = Pair(altKeyword, it)
                }
            }

            if (swappableKeyQuery != null && swappableKeyQuery!!.first != null) {
                insertAtKeyword(swappableKeyQuery!!.first!!, swappableKeyQuery!!.second)
                frequentRoot.queries.remove(swappableKeyQuery!!.second)
                frequentRoot.queries.add(query)
                return true
            }
        }
        return false
    }

    override fun toString(): String {
        return "SpatialCell(bounds=$bounds, coordinate=$_coordinatePoint, level=$_level)"
    }


    companion object {
        fun calcCoordinate(i: Int, x: Int, y: Int, granI: Int): Int {
            return (i shl 22) + y * (granI + 1) + x // Modified for confilct issue below
            // granI = 1
            // (0, 1) = 1 * 1 + 0 -> 1
            // (1, 0) = 0 * 1 + 1 -> 1

            // Alt method - return i * granMax.toDouble().pow(2).toInt() + y * granI + x // method mentioned in the paper
        }

        fun getBound(i: Int, j: Int, step: Double): Rectangle {
            if (step.isInfinite()) {
                return Rectangle(.0, .0, step, step)
            }
            return Rectangle(Point(i * step, j * step), Point((i + 1) * step, (j + 1) * step))
        }
    }

}

internal class SpatialOverlapCompartor(private var bounds: Rectangle) : Comparator<MinimalRangeQuery> {
    override fun compare(e1: MinimalRangeQuery, e2: MinimalRangeQuery): Int {
        val val1 = bounds.intersection(e1.spatialRange).area
        val val2 = bounds.intersection(e2.spatialRange).area
        return if (val1 < val2) 1 else if (val1 == val2) 0 else -1
    }
}

