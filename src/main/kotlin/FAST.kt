import com.google.gson.Gson
import constants.SpatioTextualConst
import models.*
import parsers.Place
import structures.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.log
import kotlin.math.pow

/**
 * FAST spatial-keyword index.
 */
class FAST(bounds: Rectangle, gridGran: Int) {
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
                nextLevelQueries.addAll(cell.addQuery(minKeyword, query))
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
                                // TODO - Implement query sharing
                                nextLevelQueries.addAll(cell.addQuery(minKeyword, entry.query))
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

fun evalPlaces(args: Array<String>, fast: FAST) {
    if (args.isEmpty()) {
        println("Path to Places dataset should be provided as an argument.")
        println("Run the `FASTTest` to evaluate a toy example, instead.")
        return
    }

    val queries = listOf<MinimalRangeQuery>()
    queries.forEach {
        fast.addContinuousQuery(it)
    }

    val gson = Gson()
    var oidCounter = 0
    File(args[0]).forEachLine {
        val place = gson.fromJson(it, Place::class.java)
        if (place.keywords.isNotEmpty()) {
            val obj = place.toDataObject(oidCounter, 10000)

            fast.searchQueries(obj)
            oidCounter++
        }
    }
}

fun evalToy(fast: FAST) {
    val queries = listOf(
        MinimalRangeQuery(1, listOf("k1", "k2"), Rectangle(.0, .0, 10.0, 10.0), 100),
        MinimalRangeQuery(2, listOf("k1", "k2"), Rectangle(.0, .0, 8.0, 10.0), 100),
        MinimalRangeQuery(3, listOf("k1", "k2"), Rectangle(.0, .0, 10.0, 10.0), 100),
        MinimalRangeQuery(4, listOf("k3", "k6"), Rectangle(.0, .0, 10.0, 10.0), 100),
        MinimalRangeQuery(5, listOf("k1", "k3"), Rectangle(.0, .0, 10.0, 10.0), 100),
        MinimalRangeQuery(6, listOf("k1", "k2", "k3"), Rectangle(.0, .0, 10.0, 10.0), 100),
        MinimalRangeQuery(7, listOf("k2", "k3", "k7"), Rectangle(.0, .0, 10.0, 10.0), 100),
        MinimalRangeQuery(8, listOf("k2"), Rectangle(.0, .0, 10.0, 10.0), 100),
        MinimalRangeQuery(9, listOf("k1", "k3"), Rectangle(6.0, 7.0, 10.0, 10.0), 100),
        MinimalRangeQuery(10, listOf("k1", "k3"), Rectangle(.0, .0, 4.0, 4.0), 100),
        MinimalRangeQuery(11, listOf("k1", "k3"), Rectangle(6.0, .0, 10.0, 4.0), 100),
        KNNQuery(12, listOf("k1", "k2"), Point(5.0, 5.0), 3, 100),
        KNNQuery(14, listOf("k1", "k2"), Point(7.0, 7.0), 2, 100)
    )

    queries.forEach {
        fast.addContinuousQuery(it)
    }

    val objs = listOf(
        DataObject(1, listOf("k1", "k2"), Point(7.0, 8.0), 1),
        DataObject(2, listOf("k1", "k2"), Point(5.0, 5.0), 2),
        DataObject(3, listOf("k1", "k2"), Point(2.0, 6.0), 3),
        DataObject(4, listOf("k1", "k2"), Point(1.0, 1.0), 4),
        DataObject(5, listOf("k1", "k2"), Point(5.0, 6.0), 5)
    )

    val answers = listOf(
        listOf(1, 2, 3, 8, 12, 14),
        listOf(1, 2, 3, 8, 12, 14),
        listOf(1, 2, 3, 8, 12),
        listOf(1, 2, 3, 8),
        listOf(1, 2, 3, 8, 12, 14)
    )

    objs.zip(answers).forEach { (obj, ans) ->
        print(fast.searchQueries(obj).map { it.id })
        print(" | ")
        print(ans)
        val fastAns = fast.searchQueries(obj).map { it.id }
        println(" | -> Matching: ${fastAns.size == ans.size && fastAns.toSet() == ans.toSet()}")
    }
}

fun main(args: Array<String>) {
    val fast = FAST(
        Rectangle(0.0, 0.0, SpatioTextualConst.MAX_RANGE_X, SpatioTextualConst.MAX_RANGE_Y),
        8,
    )

    evalPlaces(args, fast)
    evalToy(fast)

    fast.printIndex()
}