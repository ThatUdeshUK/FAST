import constants.SpatioTextualConst
import models.*
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

                if (context.minInsertedLevel == -1) {
                    context.maxInsertedLevel = level
                    context.minInsertedLevel = level
                }
                if (level < context.minInsertedLevel) context.minInsertedLevel = level
                if (level > context.maxInsertedLevel) context.maxInsertedLevel = level

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
                                bounds.min.y > SpatioTextualConst.MAX_RANGE_Y
                            ) continue
                            index[coordinate] = SpatialCell(context, bounds, Point(i.toDouble(), j.toDouble()), level)
                        }

                        index[coordinate]?.let { cell ->
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
            currentLevelQueries = nextLevelQueries
            level--
        }
    }

    fun searchQueries(obj: DataObject): List<MinimalRangeQuery> {
        context.objectTimeStampCounter++

        if (context.minInsertedLevel == -1) return emptyList()

        var step = if (context.maxInsertedLevel == 0) {
            context.localXStep
        } else context.localXStep * (2 shl context.maxInsertedLevel - 1)
        var granI = context.gridGran shr context.maxInsertedLevel

        var keywords = obj.keywords
        val output = mutableListOf<MinimalRangeQuery>()

        for (level: Int in context.maxInsertedLevel downTo context.minInsertedLevel) {
            if (keywords.isEmpty()) break

            val coordinates = obj.calcCoordinate(level, step, granI)
            index[coordinates]?.let {
                val (newKeywords, results) = it.searchQueries(obj, keywords)
                keywords = newKeywords
                output.addAll(results)
            }

            step /= 2
            granI = granI shl 1
        }

        return output
    }

    fun printFrequencies() {
        println("${context.keywordFrequencyMap}")
    }

    fun printIndex() {
        println("Bounds=${context.bounds}")
        index.forEach { (k, v) ->
            println("Level: $k, ${v.coordinatePoint} -->")
            v.textualIndex?.forEach { (keyword, node) ->
                printTextualNode(keyword, node)
            }
        }
    }

    private fun printTextualNode(keyword: String, node: TextualNode, level: Int = 1) {
        if (level == 4) return
        print("|" + "──".repeat(level) + "$keyword -> ")
        if (node is QueryListNode) {
            node.queries.forEach {
                print("${it.id}, ")
            }
            println()
        } else if (node is QueryTrieNode) {
            node.queries.forEach {
                print("${it.id}, ")
            }
            println()
            node.subtree.forEach { (t, u) ->
                printTextualNode(t, u, level + 1)
            }
        }
    }
}

fun main() {

}