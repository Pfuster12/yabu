package sql

import android.net.Uri
import android.provider.BaseColumns

/**
 * Contract class for the Wiki extracts SQLite database
 */
class WikiExtractsContract {

    companion object {
        val CONTENT_AUTHORITY = "com.yabu.android.yabu.wiki"
        val BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY)
        val PATH_NAME = "wikiextracts"
    }

    // Columns inner class for entries in Kanji tables.
    class WikiExtractsEntry : BaseColumns {

        companion object {
            val CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_NAME)

            val TABLE_NAME = "wikiextracts"
            val COLUMN_IS_READ = "isread"
            val COLUMN_DATE = "date"
            val COLUMN_PAGE_ID = "pageid"
            val COLUMN_TITLE = "title"
            val COLUMN_EXTRACT = "extract"
            val COLUMN_THUMBNAIL = "thumbnail"
        }

    }
}