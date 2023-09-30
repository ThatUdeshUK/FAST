package structures

class KeywordFrequency(var queryCount: Int, private var visitCount: Int, private val lastDecayTimestamp: Int) {
    override fun toString(): String {
        return "KeywordFrequency(queryCount=$queryCount, visitCount=$visitCount, lastDecayTimestamp=$lastDecayTimestamp)"
    }
}