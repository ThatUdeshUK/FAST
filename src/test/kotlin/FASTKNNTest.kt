import constants.SpatioTextualConst
import models.*
import org.junit.jupiter.api.Test

class FASTKNNTest {

    private val queries = listOf<Query>(
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
        MinimalRangeQuery(11, listOf("k1", "k3"), Rectangle(6.0, .0, 10.0, 4.0), 100)
    )

    @Test
    fun testInserts() {
        val testFAST = FAST(
            Rectangle(0.0, 0.0, SpatioTextualConst.MAX_RANGE_X, SpatioTextualConst.MAX_RANGE_Y),
            2
        )

        queries.forEach {
            testFAST.addContinuousQuery(it)
        }

        testFAST.printFrequencies()
        testFAST.printIndex()
    }

//    @Test
//    fun testSearches() {
//        throw NotImplementedError("Should implement extension first")
//    }
}