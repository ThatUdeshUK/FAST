import constants.SpatioTextualConst
import models.MinimalRangeQuery
import models.Rectangle
import org.junit.jupiter.api.Test

class FASTTest {

    private val testFAST: FAST = FAST(
        Rectangle(0.0, 0.0, SpatioTextualConst.MAX_RANGE_X, SpatioTextualConst.MAX_RANGE_Y),
        2,
        2
    )

    @Test
    fun testInserts() {
        val queries = listOf(
            MinimalRangeQuery(1, listOf("k1", "k2"), Rectangle(.0, .0, 10.0, 10.0), 10),
            MinimalRangeQuery(2, listOf("k1", "k2"), Rectangle(.0, .0, 10.0, 10.0), 10),
            MinimalRangeQuery(3, listOf("k1", "k2"), Rectangle(.0, .0, 10.0, 10.0), 10),
//            MinimalRangeQuery(4, listOf("k3", "k6"), Rectangle(.0, .0, 10.0, 10.0), 10),
//            MinimalRangeQuery(5, listOf("k1", "k3"), Rectangle(.0, .0, 10.0, 10.0), 10),
//            MinimalRangeQuery(6, listOf("k1", "k2", "k3"), Rectangle(.0, .0, 10.0, 10.0), 10),
//            MinimalRangeQuery(7, listOf("k2", "k3", "k7"), Rectangle(.0, .0, 10.0, 10.0), 10),
//            MinimalRangeQuery(8, listOf("k2"), Rectangle(.0, .0, 10.0, 10.0), 10),
//            MinimalRangeQuery(9, listOf("k3"), Rectangle(.0, .0, 10.0, 10.0), 10),
//            MinimalRangeQuery(9, listOf("k1", "k2", "k10"), Rectangle(.0, .0, 10.0, 10.0), 10)
        )

        queries.forEach {
            testFAST.addContinuousQuery(it)
        }

        testFAST.printIndex()
    }
}