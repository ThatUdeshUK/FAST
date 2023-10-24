import constants.SpatioTextualConst
import models.*
import structures.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.log
import kotlin.math.pow

/**
 * FAST spatial-keyword index.
 */
class FAST(val bounds: Rectangle, val gridGran: Int) {
    private val context: Context = Context(bounds, gridGran, log(gridGran.toDouble(), 2.0).toInt())
    private val index: ConcurrentHashMap<Int, SpatialCell> = ConcurrentHashMap()

    fun addContinuousQuery(query: Query) {
        context.queryTimeStampCounter++

        when (query) {
            is MinimalRangeQuery -> addContinuousMinimalRangeQuery(query)
            is KNNQuery -> addContinuousKNNQuery(query)
        }
    }

    private fun addContinuousKNNQuery(query: KNNQuery) {
        if (context.minInsertedLevel == -1) {
            context.maxInsertedLevel = context.maxLevel
            context.minInsertedLevel = context.maxLevel
        }

        val minKeyword = getMinKeyword(context.maxLevel, query)

        val coordinate = SpatialCell.calcCoordinate(context.maxLevel, 0, 0, 1)
        if (!index.containsKey(coordinate)) {
            val bounds = SpatialCell.getBound(0, 0, SpatioTextualConst.MAX_RANGE_X)
            index[coordinate] = SpatialCell(context, bounds, Point(0.0, 0.0), context.maxLevel)
        }

        val nextLevelQueries: ArrayList<ReinsertEntry> = ArrayList()
        index[coordinate]?.let { cell ->
            if (cell.overlapsSpatially(query.location)) {
                nextLevelQueries.addAll(cell.addQuery(minKeyword, query).second)
            }
            if (cell.textualIndex.isNullOrEmpty()) {
                index.remove(coordinate)
            }
        }
        reinsertContinuous(nextLevelQueries, atLevel = context.maxLevel - 1)
    }

    private fun addContinuousMinimalRangeQuery(query: MinimalRangeQuery) {
        reinsertContinuous(listOf(ReinsertEntry(query)))
    }

    private fun reinsertContinuous(queries: List<ReinsertEntry>, atLevel: Int = context.maxLevel) {
        var level = atLevel

        var currentLevelQueries: MutableList<ReinsertEntry> = queries.toMutableList()

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

                val minKeyword = getMinKeyword(level, entry.query)
                var sharedNodes: QueryListNode? = null

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
                                if (i == minX && j == minY) {
                                    val (sharedPendingNodes, pendingQueries) = cell.addQuery(minKeyword, entry.query)
                                    sharedNodes = sharedPendingNodes
                                    nextLevelQueries.addAll(pendingQueries)
                                } else if (sharedNodes != null) {
                                    val pendingQueries = cell.addSharedQuery(minKeyword, entry.query, sharedNodes)
                                    nextLevelQueries.addAll(pendingQueries)
                                } else {
                                    val (_, pendingQueries) = cell.addQuery(minKeyword, entry.query)
                                    nextLevelQueries.addAll(pendingQueries)
                                }
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

    fun searchQueries(obj: DataObject): List<Query> {
        context.objectTimeStampCounter++

        if (context.minInsertedLevel == -1) return emptyList()

        var step = if (context.maxInsertedLevel == 0) {
            context.localXStep
        } else context.localXStep * (2 shl context.maxInsertedLevel - 1)
        var granI = context.gridGran shr context.maxInsertedLevel

        var keywords = obj.keywords
        val output = mutableListOf<Query>()

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

    private fun getMinKeyword(level: Int, query: Query): String {
        var minKeyword: String? = null
        var minCount: Int = Int.MAX_VALUE

        query.keywords.forEach { keyword ->
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

        return minKeyword!!
    }

    fun printFrequencies() {
        println("${context.keywordFrequencyMap}")
    }

    fun printIndex() {
        println("Bounds=${context.bounds}")
        index.forEach { (k, v) ->
            println("Level: ${v.level}, Loc: ${v.coordinatePoint}, Key: $k -->")
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
