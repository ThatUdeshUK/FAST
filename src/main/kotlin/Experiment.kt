import com.google.gson.Gson
import constants.SpatioTextualConst
import extensions.countLines
import models.*
import parsers.Place
import java.io.File

fun evalPlaces(filePath: String?, numQueries: Int) {
    if (filePath == null) {
        println("Path to Places dataset should be provided as an argument.")
        return
    }

    val fast = FAST(
        Rectangle(0.0, 0.0, SpatioTextualConst.MAX_RANGE_X, SpatioTextualConst.MAX_RANGE_Y),
        128,
    )

    val file = File(filePath)
    println("Lines in Places US: ${file.countLines()}")

    print("Parsing the Places file -> ")
    val gson = Gson()
    val places: MutableList<Place> = mutableListOf()
    file.forEachLine {
        val place = gson.fromJson(it, Place::class.java)
        if (place.keywords.isNotEmpty()) {
            places.add(place)
        }
    }
    println("Done!")

    print("Shuffling -> ")
    places.shuffle()
    println("Done!")

    val queries = places.subList(0, numQueries).mapIndexed { index, place ->
        place.toQuery(index, 100, 12993248)
    }

    val objs = places.subList(numQueries + 1, places.size).mapIndexed { index, place ->
        place.toDataObject(index, 12993248)
    }

    queries.forEach {
        fast.addContinuousQuery(it)
    }

    objs.forEach {
        fast.searchQueries(it)
    }
}

fun evalToy() {
    val fast = FAST(Rectangle(0.0, 0.0, 10.1, 10.1), 8)

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

    fast.printIndex()
}

fun main(args: Array<String>) {
    val experimentName = args.getOrElse(0) { "toy" }
    when (experimentName) {
        "places" -> evalPlaces(args.getOrNull(1), 1000000)
        "toy" -> evalToy()
    }
}