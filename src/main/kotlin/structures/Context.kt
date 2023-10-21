package structures

import constants.SpatioTextualConst
import models.Rectangle

class Context(val bounds: Rectangle, val gridGran: Int, val maxLevel: Int) {
    val localXStep: Double = SpatioTextualConst.MAX_RANGE_X / gridGran

    var minInsertedLevel: Int = -1
    var maxInsertedLevel: Int = -1

//    val minInsertedLevelInterleaved: Int = -1
//    val maxInsertedLevelInterleaved: Int = -1

    var numberOfHashEntries: Int = 0
    var numberOfTrieNodes: Int = 0
    var numberOfInsertedTextualNodes: Int = 0
    var numberOfObjectSearchHashAccess: Int = 0
    var numberOfObjectSearchTrieNodeAccess: Int = 0
    var numberOfObjectSearchInvListNode: Int = 0
    var numberOfObjectSearchTrieNode: Int = 0

    var queryTimeStampCounter: Int = 0
    var objectTimeStampCounter: Int = 0

    val trieSplitThreshold: Int = SpatioTextualConst.TRIE_SPLIT_THRESHOLD
    val degradationRatio: Int = SpatioTextualConst.DEGRADATION_RATIO

    val keywordFrequencyMap: HashMap<String, KeywordFrequency> = HashMap()
}