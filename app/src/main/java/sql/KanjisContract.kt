package sql

import android.net.Uri
import android.provider.BaseColumns

/**
 * Contract class for the Kanjis SQLite database
 */
class KanjisContract {

    companion object {
        val CONTENT_AUTHORITY = "com.yabu.android.yabu"
        val BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY)
        val PATH_NAME = "kanjis"
    }

    // Columns inner class for entries in Kanji tables.
    class KanjisEntry : BaseColumns {

        companion object {
            val CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_NAME)

            val TABLE_NAME = "kanjis"
            val COLUMN_KANJI_WORD = "word"
            val COLUMN_KANJI_READING = "reading"
            val COLUMN_KANJI_PARTS_OF_SPEECH = "parts_of_speech"
            val COLUMN_IS_COMMON = "common"
            val COLUMN_JLPT = "jlpt"
            val COLUMN_DEFINITION_1 = "definition_1"
            val COLUMN_DEFINITION_2 = "definition_2"
            val COLUMN_URL = "url"
            val COLUMN_IS_REVIEW = "review"
            val COLUMN_RANGE = "range"
        }

    }
}