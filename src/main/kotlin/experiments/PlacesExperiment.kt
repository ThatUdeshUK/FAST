package experiments

import FAST
import com.google.gson.Gson
import constants.SpatioTextualConst
import models.*
import parsers.Place
import java.io.File
import kotlin.time.measureTime

class PlacesExperiment(
    outputPath: String,
    private val inputPath: String,
    private val numQueries: Int,
    private val numObjects: Int,
    private val srRate: Double = 0.01
) : Experiment(
    "places",
    outputPath,
    FAST(Rectangle(0.0, 0.0, SpatioTextualConst.MAX_RANGE_X, SpatioTextualConst.MAX_RANGE_Y), 512)
) {
    private val places: MutableList<Place> = mutableListOf()
    private var minX: Double = Double.MAX_VALUE
    private var maxX: Double = -Double.MAX_VALUE
    private var minY: Double = Double.MAX_VALUE
    private var maxY: Double = -Double.MAX_VALUE

    private fun loadData() {
        val file = File(inputPath)

        print("Parsing the Places file -> ")
        val fileReadTime = measureTime {
            val gson = Gson()
            file.forEachLine {
                val place = gson.fromJson(it, Place::class.java)
                if (place.keywords.isNotEmpty()) {
                    if (place.coordinate.x < minX) minX = place.coordinate.x
                    if (place.coordinate.x > maxX) maxX = place.coordinate.x
                    if (place.coordinate.y < minY) minY = place.coordinate.y
                    if (place.coordinate.y > maxY) maxY = place.coordinate.y

                    places.add(place)
                }
            }
        }
        println("Done! Time=$fileReadTime")

        places.forEach { it.scale(Point(minX, minY), Point(maxX, maxY), SpatioTextualConst.MAX_RANGE_X) }

        print("Shuffling -> ")
        val shuffleTime = measureTime {
            places.shuffle()
        }
        println("Done! Time=$shuffleTime")
    }

    override fun generateQueries(): List<Query> {
        val r = (SpatioTextualConst.MAX_RANGE_X * srRate).toInt()
        return places.subList(0, numQueries).mapIndexed { index, place ->
            place.toQuery(index, r, numQueries + numObjects + 1)
        }
    }

    override fun generateObjects(): List<DataObject> {
        return places.subList(numQueries + 1, numQueries + numObjects).mapIndexed { index, place ->
            place.toDataObject(index, numQueries + numObjects + 1)
        }
    }

    override fun run() {
        loadData()
        createAndSearch()
        save(
            mapOf(
                Pair("num_queries", numQueries.toString()),
                Pair("num_objects", numObjects.toString()),
                Pair("sr_rate", srRate.toString())
            )
        )
    }
}