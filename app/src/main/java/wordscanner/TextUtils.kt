package wordscanner

import android.text.TextUtils
import java.util.logging.Logger

/**
 * Helper utils to make extract text spannable, scan the text for kanji and extract them
 * for api calls, and set onClicks on the different words extracted.r
 */
class TextUtils {

    /**
     * Companion object to expose utils to other classes.
     */
    companion object {

        /*
         The Unicode code-point number range for UTF-16 encoding (see https://codepoints.net/)
         for the 'CJK Unified Ideographs', aka the Chinese and Japanese characters. This range
         helps identifies whether a Character object is a kanji in our wiki extract text
         */
        val range = object {
            // Start of the range
            val startCodePoint = 19968
            // End of the range
            val endCodePoint =  40917
        }

        fun scanText(string: String) : MutableList<String> {
            val test1 = Character.codePointAt("ン", 0)
            val test2 = Character.getName(12531)

            Logger.getLogger("Tag").warning(test1.toString())
            Logger.getLogger("Tag").warning(test2.toString())

            return mutableListOf()
        }
    }


}