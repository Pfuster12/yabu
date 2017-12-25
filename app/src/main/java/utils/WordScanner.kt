package utils

/**
 * Helper util to make extract text spannable, scan the text for kanji and extract them
 * for api calls, and set onClicks on the different words extracted.r
 */
class WordScanner {

    /**
     * Companion object to expose utils to other classes.
     */
    companion object {
        // function to get an instance of the utils
        fun getUtils(): WordScanner {
            return WordScanner()
        }
    }

    /**
     * Helper function to scan the text extract and retrieve the character sequences
     * that represent unicode code-points within the CJK Unified Ideographs block of code
     * points, aka the Kanji in the text. Return a list of these strings.
     */
    fun scanText(extract: String?): MutableList<Pair<IntRange, String>> {
        // init an empty pair list of the index range and the word.
        val pairIndexWordList = mutableListOf<Pair<IntRange, String>>()

        // Iterate through the chars in the extract string
        if (extract != null) {
            for (i in 0 until extract.length) {

                // Grab the code-point of the character
                val codePoint = Character.codePointAt("${extract[i]}", 0)

                // Check the Unicode range and join the kanji words
                // Put into the map the index of the kanji and the word as well.
                pairIndexWordList.addAll(checkUnicodeRange(i, codePoint, extract))
            }
        }

        // Return the list of kanji strings
        return pairIndexWordList
    }

    /**
     * Private helper function to check what Unicode block from the above ranges
     * the chars code-point falls in.
     */
    private fun checkUnicodeRange(index: Int, codePoint: Int, string: String)
            : MutableList<Pair<IntRange, String>> {
        /*
       The Unicode code-point number range for UTF-16 encoding (see https://codepoints.net/)
       for the 'CJK Unified Ideographs', aka the Chinese and Japanese characters. This range
       helps identifies whether a Character object is a Kanji in our wiki extract text
       */
        val cjkRange = object {
            // Start of the range
            val startCodePoint = 19968 // Represents the character 一 "one"
            // End of the range
            val endCodePoint =  40917 // Almost the last one represents 鿕.
            // Special iteration mark code point
            val iterationCodePoint = 12293
        }

        // Unicode code-point range for punctuation of CJK unicode chars
        val punctuationRange = object {
            // Start of the range
            val startCodePoint = 12288
            // End of the range
            val endCodePoint = 12351
        }

        // Unicode code-point range for the Hiragana unicode chars
        val hiraganaRange = object {
            // Start of the range
            val startCodePoint = 12353
            // End of the range
            val endCodePoint = 12447
        }

        // Unicode code-point range for the Katakana unicode chars
        val katakanaRange = object {
            // Start of the range
            val startCodePoint = 12448
            // End of the range
            val endCodePoint = 12543
        }

        // Unicode code-point range for the Katakana extensions unicode chars
        val katakanaExtensionRange = object {
            // Start of the range
            val startCodePoint = 12784
            // End of the range
            val endCodePoint = 12799
        }

        // init an empty pair list
        val pairIndexWordList = mutableListOf<Pair<IntRange, String>>()

        /*
        * Range control flow to check what the chars code-points are (Kanji, hiragana,
        * katakana, punctuation or other.
        */
        if (codePoint in cjkRange.startCodePoint..cjkRange.endCodePoint
                || codePoint == cjkRange.iterationCodePoint) {

            // Init an empty string to contain the full Kanji word extracted below.
            val pairIndexWordListOneEntry = joinKanji(index, string)

            // Add the pair given by joinKanji() that is not empty.
            if (pairIndexWordListOneEntry.any()) {
                pairIndexWordList.addAll(pairIndexWordListOneEntry)
            }
        }

    /*    if (codePoint in hiraganaRange.startCodePoint..hiraganaRange.endCodePoint) {
            // The code-point falls in the hiragana block.
        }

        if (codePoint in katakanaRange.startCodePoint..katakanaRange.endCodePoint) {
            // The code-point falls in the katakana block.
        }

        if (codePoint in katakanaExtensionRange.startCodePoint..katakanaExtensionRange.endCodePoint) {
            // The code-point falls in the katakana extension block.
        }

        if (codePoint in punctuationRange.startCodePoint..punctuationRange.endCodePoint) {
            // The code-point falls in the punctuation block.
        }
*/
        // Return the map of values of the index of the kanji and the word itself.
        return pairIndexWordList
    }

    /**
     * Helper function to find what kanji to join or not as delimited by hiragana and katana
     * returning a string of the word kanji.
     */
    private fun joinKanji(index: Int, string: String): MutableList<Pair<IntRange, String>> {
        // init an empty pair list
        val pairIndexWordList = mutableListOf<Pair<IntRange, String>>()

        // The code-point falls in the CJK ideograph block.
        // Check previous char
        var isPrevCJK = isPreviousIndexCJK(index, string)
        // Check after char
        val isAfterCJK = isAfterIndexCJK(index, string)

        if (isAfterCJK) {
            // Do nothing and let the loop continue as it is not the end of a kanji word.
        } else {
            // The char after is not a CJK therefore we know this is the end of a kanji word.
            // Get the current kanji
            var kanjiReversed = string[index].toString()

            // Get the current index
            var currentIndex = index

            // init an index list to hold all the indexes of the word we are combining here. Start
            // with the first one (In reverse).
            val indexList = mutableListOf(currentIndex)

            while (isPrevCJK) {
                // While the previous char is a CJK char, join the previous char
                // with the current one which we know is the last of the word.
                val prevKanji = string[currentIndex - 1]

                // This will return a reversed kanji word.
                kanjiReversed = kanjiReversed.plus(prevKanji)

                // Decrement the index to check the next char down the line.
                currentIndex = currentIndex.dec()

                // Add the next index
                indexList.add(currentIndex)

                // Check if it is CJK and iterate while loop
                isPrevCJK = isPreviousIndexCJK(currentIndex, string)
            }

            // Create an int range to store the index range of the kanji in the text string.
            val indexRange = IntRange(indexList.first(), indexList.last())

            // Create a new pair value with the correct word orientation.
            val pairIndexWord = Pair(indexRange, kanjiReversed.reversed())
            pairIndexWordList.add(pairIndexWord)
        }
        return pairIndexWordList
    }

    /**
     * Helper fun to return whether code point of char
     * BEFORE the current one is within the CJK block
     */
    private fun isPreviousIndexCJK(currentIndex: Int, string: String): Boolean {
        // Get the previous index
        val previousIndex = currentIndex - 1
        var isCJK = false

        // Get cjkRange
        val cjkRange = object {
            // Start of the range
            val startCodePoint = 19968 // Represents the character 一 "one"
            // End of the range
            val endCodePoint =  40917 // Almost the last one represents 鿕. (?)
            // Iteration mark code point
            val iterationCodePoint = 12293
        }

        // Grab the code-point of the previous character when the index is not 0
        if (currentIndex != 0) {
            val codePoint = Character.codePointAt(string[previousIndex].toString(), 0)
            if (codePoint in cjkRange.startCodePoint..cjkRange.endCodePoint
                    || codePoint == cjkRange.iterationCodePoint) {
                // The code-point falls in the CJK ideograph block.
                // Add char to the kanji list
                isCJK = true
            }
        }

        // Return boolean
        return isCJK
    }

    /**
     * Helper fun to return whether code point of char
     * AFTER the current one is within the CJK block
     */
    private fun isAfterIndexCJK(currentIndex: Int, string: String): Boolean {
        // Get the index after
        val afterIndex = currentIndex + 1
        var isCJK = false

        // Get cjkRange
        val cjkRange = object {
            // Start of the range
            val startCodePoint = 19968 // Represents the character 一 "one"
            // End of the range
            val endCodePoint =  40917 // Almost the last one represents 鿕. (?)
            // Iteration mark code point
            val iterationCodePoint = 12293
        }

        // Grab the code-point of the after character when it is not the last.
        if (currentIndex < string.length) {
            val codePoint = Character.codePointAt(string[afterIndex].toString(), 0)
            if (codePoint in cjkRange.startCodePoint..cjkRange.endCodePoint
                    || codePoint == cjkRange.iterationCodePoint) {
                // The code-point falls in the CJK ideograph block.
                // Add char to the kanji list
                isCJK = true
            }
        }

        // Return boolean
        return isCJK
    }
}