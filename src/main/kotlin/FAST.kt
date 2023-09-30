import constants.SpatioTextualConst
import models.MinimalRangeQuery
import models.Rectangle
import structures.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow

/**
 * FAST spatial-keyword index.
 */
class FAST(private val bounds: Rectangle, private val gridGran: Int, private val maxLevel: Int) {
    private val index: ConcurrentHashMap<Int, SpatialCell> = ConcurrentHashMap()
    private val keywordFrequencyMap: HashMap<String, KeywordFrequency> = HashMap()

//    val localXStep: Double = SpatioTextualConst.MAX_RANGE_X / gridGran
//    val localYStep: Double = SpatioTextualConst.MAX_RANGE_Y / gridGran
//
//    var minInsertedLevel: Int = -1
//    var maxInsertedLevel: Int = -1
//
//    val minInsertedLevelInterleaved: Int = -1
//    val maxInsertedLevelInterleaved: Int = -1

    private var queryTimeStampCounter: Int = 0
    private var objectTimeStampCounter: Int = 0

    fun addContinuousQuery(query: MinimalRangeQuery) {
        queryTimeStampCounter.inc()

        var level = maxLevel

        var currentLevelQueries: ArrayList<MinimalRangeQuery> = ArrayList()
        currentLevelQueries.add(query)

        while (level >= 0 && currentLevelQueries.size > 0) {
//            val nextLevelQueries: ArrayList<MinimalRangeQuery> = ArrayList()
            val levelGran = (gridGran / 2.0.pow(level)).toInt() // calculate the gran_i (eq. 1)
            val levelStep = SpatioTextualConst.MAX_RANGE_X / levelGran // calculate the side_len_i (alt to eq. 2)
            currentLevelQueries.forEach { entry ->
                val (minX, minY, maxX, maxY) = entry.spatialRange.mapToCell(levelStep) // Map bounds to spatial cell
//                val span = max(maxX - minX, maxY - minX)

//                if (minInsertedLevel == -1) {
//                    maxInsertedLevel = level
//                    minInsertedLevel = level
//                }
//                if (level < minInsertedLevel) minInsertedLevel = level
//                if (level > maxInsertedLevel) maxInsertedLevel = level
                var minKeyword: String? = null
                var minCount: Int = Int.MAX_VALUE

                entry.keywords.forEach { keyword ->
                    var stats = keywordFrequencyMap[keyword]
                    if (stats != null) {
                        if (level == maxLevel) stats.queryCount++ // TODO: CLARIFY THIS!!!
                        if (stats.queryCount < minCount) {
                            minKeyword = keyword
                            minCount = stats.queryCount
                        }
                    } else {
                        stats = KeywordFrequency(1, 1, objectTimeStampCounter)
                        keywordFrequencyMap[keyword] = stats

                        if (minCount != 0) {
                            minKeyword = keyword
                            minCount = 0
                        }
                    }
                }

                for (i: Int in minX..maxX) {
                    for (j: Int in minY..maxY) {
                        val coordinate = SpatialCell.calcCoordinate(level, i, j, levelGran)

                        if (!index.containsKey(coordinate)) {
                            val bounds = SpatialCell.getBound(i, j, levelStep)
                            if (bounds.min.x > SpatioTextualConst.MAX_RANGE_X ||
                                bounds.min.y > SpatioTextualConst.MAX_RANGE_Y) continue
                            index[coordinate] = SpatialCell(bounds, coordinate, level)
                        }

                        index[coordinate]?.let {cell ->
                            if (cell.overlapsSpatially(entry.spatialRange)) {
                                if (i == minX && j == minY) {
                                    cell.addQuery(minKeyword!!, query)
                                } else {
                                    // TODO - Add non max level queries
                                    print("Not inserting to lower levels!!")
                                }
                            }
                            if (cell.textualIndex.isNullOrEmpty()) {
                                index.remove(coordinate)
                            }
                        }
                    }
                }
            }
            // TODO - Add next level queries to current level queries
            currentLevelQueries = ArrayList()
            level--
        }
    }

    fun printFrequencies() {
        println("$keywordFrequencyMap")
    }

    private fun printTextualNode(keyword: String, node: TextualNode, level: Int=1) {
        if (level == 4) return
        print("|" +"──".repeat(level) + "$keyword -> ")
        if (node is QueryListNode) {
            node.queries.forEach {
                print("${it.queryId}, ")
            }
            println()
        } else if (node is QueryTrieNode) {
            node.queries.forEach {
                print("${it.queryId}, ")
            }
            println()
            node.subtree.forEach { (t, u) ->
                printTextualNode(t, u, level+1)
            }
        }
    }

    fun printIndex() {
        println("Bounds=$bounds")
        index.forEach { (k, v) ->
            println("Level: $k -->")
            v.textualIndex?.forEach { (keyword, node) ->
                printTextualNode(keyword, node)
            }
        }
    }

    companion object {
        var numberOfHashEntries: Int = 0
        var numberOfTrieNodes: Int = 0
        var numberOfInsertedTextualNodes: Int = 0

        var trieSplitThreshold: Int = SpatioTextualConst.TRIE_SPLIT_THRESHOLD
    }
}

fun main() {
    val fast = FAST(
        Rectangle(0.0, 0.0, SpatioTextualConst.MAX_RANGE_X, SpatioTextualConst.MAX_RANGE_Y),
        2,
        2
    )

    val queries = listOf(
        MinimalRangeQuery(1, listOf("k1", "k2"), Rectangle(.0, .0, 10.0, 10.0), 10),
        MinimalRangeQuery(2, listOf("k1", "k2"), Rectangle(.0, .0, 10.0, 10.0), 10),
        MinimalRangeQuery(3, listOf("k1", "k2"), Rectangle(.0, .0, 10.0, 10.0), 10),
        MinimalRangeQuery(4, listOf("k3", "k6"), Rectangle(.0, .0, 10.0, 10.0), 10),
        MinimalRangeQuery(5, listOf("k1", "k3"), Rectangle(.0, .0, 10.0, 10.0), 10),
        MinimalRangeQuery(6, listOf("k1", "k2", "k3"), Rectangle(.0, .0, 10.0, 10.0), 10),
        MinimalRangeQuery(7, listOf("k2", "k3", "k7"), Rectangle(.0, .0, 10.0, 10.0), 10),
        MinimalRangeQuery(8, listOf("k2"), Rectangle(.0, .0, 10.0, 10.0), 10),
        MinimalRangeQuery(9, listOf("k3"), Rectangle(.0, .0, 10.0, 10.0), 10),
    )

    queries.forEach {
        println("Inserting: ${it.queryId}")
        fast.addContinuousQuery(it)
        fast.printFrequencies()
        fast.printIndex()
        println("------------------------")
    }

}