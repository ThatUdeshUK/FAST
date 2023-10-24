package experiments

import FAST
import models.*

class ToyExperiment(outputPath: String?) : Experiment(
    "toy",
    outputPath,
    FAST(Rectangle(0.0, 0.0, 10.1, 10.1), 8)
) {

    override fun generateQueries(): List<Query> {
        return listOf(
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
    }

    override fun generateObjects(): List<DataObject> {
        return listOf(
            DataObject(1, listOf("k1", "k2"), Point(7.0, 8.0), 1),
            DataObject(2, listOf("k1", "k2"), Point(5.0, 5.0), 2),
            DataObject(3, listOf("k1", "k2"), Point(2.0, 6.0), 3),
            DataObject(4, listOf("k1", "k2"), Point(1.0, 1.0), 4),
            DataObject(5, listOf("k1", "k2"), Point(5.0, 6.0), 5)
        )
    }

    override fun run() {
        val answers = listOf(
            listOf(1, 2, 3, 8, 12, 14),
            listOf(1, 2, 3, 8, 12, 14),
            listOf(1, 2, 3, 8, 12),
            listOf(1, 2, 3, 8),
            listOf(1, 2, 3, 8, 12, 14)
        )

        val fastAnswers = createAndSearch()

        fastAnswers.zip(answers).forEach { (fastAns, ans) ->
            print("${fastAns.map { it.id }} | $ans")
            val fastAnsIDs = fastAns.map { it.id }
            println(" | -> Matching: ${fastAnsIDs.size == ans.size && fastAnsIDs.toSet() == ans.toSet()}")
        }

        println("\nCreation time: ${this.creationTime}")
        println("\nSearch time: ${this.searchTime}\n")

        printIndex()
    }
}