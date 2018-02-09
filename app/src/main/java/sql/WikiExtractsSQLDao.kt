package sql

import android.content.ContentValues
import android.content.Context
import android.provider.BaseColumns
import jsondataclasses.WikiExtract
import jsondataclasses.WikiThumbnail
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import sql.WikiExtractsContract.WikiExtractsEntry
import java.util.concurrent.Future

/**
 * Class to contain helper methods to save internet loaded information into the
 * wiki extracts database for a persistent model.
 */
class WikiExtractsSQLDao {

    companion object {
        fun getInstance(): WikiExtractsSQLDao {
            return WikiExtractsSQLDao()
        }
    }

    // init an executor for cursor calls
    val executor: ExecutorService = Executors.newCachedThreadPool()

    /**
     * Function to check whether the articles loaded are from today's featured page. If not,
     * it will trigger a refresh to load the most recent ones from the web.
     */
    fun isToday(context: Context): Boolean {
        var isTodayBool = false
        // set the query params
        val projection = arrayOf(BaseColumns._ID,
                WikiExtractsEntry.COLUMN_DATE)
        val selection = WikiExtractsEntry.COLUMN_DATE + " = ?"
        val today = java.util.Calendar.getInstance().get(java.util.Calendar.DATE)
        val selArgs = arrayOf(today.toString())
        val cursor = context.contentResolver.query(WikiExtractsEntry.CONTENT_URI, projection, selection,
                selArgs, null, null)

        // move to results
        cursor.moveToFirst()
        // check for empty cursor
        if (cursor != null && cursor.count > 0){
            // cycle through cursor
            var date = -1
            // Check for null date entry
            if (!cursor.isNull(cursor.getColumnIndex(WikiExtractsEntry.COLUMN_DATE))) {
                date = cursor.getInt(cursor.getColumnIndex(WikiExtractsEntry.COLUMN_DATE))
            }

            // check if date saved is the same day as today
            if (today == date) {
                isTodayBool = true
            }
        }
        if (!cursor.isClosed) {
            cursor.close()
        }

        return isTodayBool
    }

    /**
     * Save the wiki extract into the database with a time stamp
     */
    fun saveWikiExtracts(context: Context, wikiExtracts: List<WikiExtract>?): Int {
        // get todays day
        val today = java.util.Calendar.getInstance().get(java.util.Calendar.DATE)

        // init a list of content values
        val values = mutableListOf<ContentValues>()

        // loop through the extracts to make an array of content of values
        if (wikiExtracts != null) {
            for (extract in wikiExtracts) {
                val value = ContentValues()
                value.put(WikiExtractsEntry.COLUMN_DATE, today)
                value.put(WikiExtractsEntry.COLUMN_PAGE_ID, extract.pageId)
                value.put(WikiExtractsEntry.COLUMN_TITLE, extract.title)
                value.put(WikiExtractsEntry.COLUMN_EXTRACT, extract.extract)
                value.put(WikiExtractsEntry.COLUMN_THUMBNAIL, extract.thumbnail?.source)

                values.add(value)
            }
        }
        // change the list to an array
        val arrayOfValues = values.toTypedArray()

        // perform bulk insert to database to save the wiki extracts. returns rows added.
        return context.contentResolver.bulkInsert(null, arrayOfValues)
    }

    fun getWikiExtracts(context: Context): MutableList<WikiExtract> {
        // init a list of wikiextracts
        val wikiExtracts = mutableListOf<WikiExtract>()

        val future: Future<MutableList<WikiExtract>> = executor.submit<MutableList<WikiExtract>> {
            // Set the query params for the definition query.
            val projection = arrayOf(BaseColumns._ID,
                    WikiExtractsEntry.COLUMN_PAGE_ID,
                    WikiExtractsEntry.COLUMN_TITLE,
                    WikiExtractsEntry.COLUMN_EXTRACT,
                    WikiExtractsEntry.COLUMN_THUMBNAIL)

            val cursor = context.contentResolver.query(WikiExtractsEntry.CONTENT_URI, projection, null,
                    null, null, null)

            cursor.moveToFirst()
            // Check for empty cursor
            if (cursor != null && cursor.count > 0){
                do {
                    // Grab all the new data
                    val pageId = cursor.getInt(cursor.getColumnIndex(WikiExtractsEntry.COLUMN_PAGE_ID))
                    val title = cursor.getString(cursor.getColumnIndex(WikiExtractsEntry.COLUMN_TITLE))
                    val extract = cursor.getString(cursor.getColumnIndex(WikiExtractsEntry.COLUMN_EXTRACT))
                    val thumbnail = cursor.getString(cursor.getColumnIndex(WikiExtractsEntry.COLUMN_THUMBNAIL))

                    // Create the new wiki extract with data from database
                    val wikiExtract = WikiExtract(pageId, title, extract, WikiThumbnail(thumbnail))
                    wikiExtracts.add(wikiExtract)
                } while (cursor.moveToNext())
            }
            if (!cursor.isClosed) {
                cursor.close()
            }

            return@submit wikiExtracts
        }

        // return the live data object.
        return future.get()
    }
}