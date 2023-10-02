import constants.SpatioTextualConst
import models.MinimalRangeQuery
import models.Rectangle
import models.ReinsertEntry
import structures.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow

/**
 * FAST spatial-keyword index.
 */
class FAST(bounds: Rectangle, gridGran: Int, maxLevel: Int) {
    private val context: Context = Context(bounds, gridGran, maxLevel)
    private val index: ConcurrentHashMap<Int, SpatialCell> = ConcurrentHashMap()

    fun addContinuousQuery(query: MinimalRangeQuery) {
        context.queryTimeStampCounter++

        var level = context.maxLevel

        var currentLevelQueries: ArrayList<ReinsertEntry> = ArrayList()
        currentLevelQueries.add(ReinsertEntry(query))

        while (level >= 0 && currentLevelQueries.size > 0) {
            val nextLevelQueries: ArrayList<ReinsertEntry> = ArrayList()
            val levelGran = (context.gridGran / 2.0.pow(level)).toInt() // calculate the gran_i (eq. 1)
            val levelStep = SpatioTextualConst.MAX_RANGE_X / levelGran // calculate the side_len_i (alt to eq. 2)
            currentLevelQueries.forEach { entry ->
                val (minX, minY, maxX, maxY) = entry.range.mapToCell(levelStep) // Map bounds to spatial cell
//                val span = max(maxX - minX, maxY - minX)

//                if (minInsertedLevel == -1) {
//                    maxInsertedLevel = level
//                    minInsertedLevel = level
//                }
//                if (level < minInsertedLevel) minInsertedLevel = level
//                if (level > maxInsertedLevel) maxInsertedLevel = level
                var minKeyword: String? = null
                var minCount: Int = Int.MAX_VALUE

                entry.query.keywords.forEach { keyword ->
                    var stats = context.keywordFrequencyMap[keyword]
                    if (stats != null) {
                        if (level == context.maxLevel) stats.queryCount++
                        if (stats.queryCount < minCount) {
                            minKeyword = keyword
                            minCount = stats.queryCount
                        }
                    } else {
                        stats = KeywordFrequency(1, 1, context.objectTimeStampCounter)
                        context.keywordFrequencyMap[keyword] = stats

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
                            index[coordinate] = SpatialCell(context, bounds, coordinate, level)
                        }

                        index[coordinate]?.let {cell ->
                            if (cell.overlapsSpatially(entry.query.spatialRange)) {
//                                if (i == minX && j == minY) {
                                // TODO - Implement query sharing
                                nextLevelQueries.addAll(cell.addQuery(minKeyword!!, entry.query))
//                                } else {
//                                    print("Not inserting to lower levels!!")
//                                }
                            }
                            if (cell.textualIndex.isNullOrEmpty()) {
                                index.remove(coordinate)
                            }
                        }
                    }
                }
            }
            if (nextLevelQueries.isNotEmpty()) {
                println("Potential next list: $nextLevelQueries")
            }
            currentLevelQueries = nextLevelQueries
            level--
        }
    }

    fun printFrequencies() {
        println("${context.keywordFrequencyMap}")
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
        println("Bounds=${context.bounds}")
        index.forEach { (k, v) ->
            println("Level: $k -->")
            v.textualIndex?.forEach { (keyword, node) ->
                printTextualNode(keyword, node)
            }
        }
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