package experiments

import FAST
import exceptions.InvalidOutputFile
import models.DataObject
import models.Query
import java.io.File
import kotlin.time.Duration
import kotlin.time.measureTime

abstract class Experiment(private val name: String, private val outputPath: String?, private val fast: FAST) {
    var creationTime: Duration = Duration.ZERO
    var searchTime: Duration = Duration.ZERO

    protected abstract fun generateQueries(): List<Query>

    protected abstract fun generateObjects(): List<DataObject>

    abstract fun run()

    protected fun createAndSearch(): Iterable<List<Query>> {
        creationTime = measureTime {
            generateQueries().forEach {
                fast.addContinuousQuery(it)
            }
        }

        var results: Iterable<List<Query>>?
        searchTime = measureTime {
            results = generateObjects().map { fast.searchQueries(it) }
        }
        return results!!
    }

    fun printIndex() {
        fast.printIndex()
    }

    protected fun save(additional: Map<String, String>) {
        if (outputPath == null) {
            println("No output file specified. Skipping saving!")
            return
        }

        val outputFile = File(outputPath)

        if (!outputFile.isDirectory()) {
            if (!outputFile.exists()) {
                var header = "name,creation_time,search_time,gran,max_x,max_y"
                additional.keys.forEach { header += ",$it" }
                outputFile.writeText(header + "\n")
            }

            var line = "$name,${creationTime.inWholeMicroseconds},${searchTime.inWholeMicroseconds},${fast.gridGran}," +
                    "${fast.bounds.max.x},${fast.bounds.max.y}"
            additional.values.forEach { line += ",$it" }
            outputFile.appendText(line + "\n")
        } else {
            throw InvalidOutputFile(outputPath)
        }
    }

}