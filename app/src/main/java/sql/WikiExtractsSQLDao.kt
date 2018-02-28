package sql

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.BaseColumns
import jsondataclasses.WikiExtract
import jsondataclasses.WikiThumbnail
import repository.WikiExtractRepository
import sql.WikiExtractsContract.WikiExtractsEntry
import java.util.concurrent.*
import java.util.logging.Logger

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
     * Function to check whether the article loaded has been marked as read
     */
    fun isRead(context: Context, wikiExtract: WikiExtract?): Boolean? {
        var future: Future<Boolean>? = null

        try {
            future =
                    WikiExtractRepository.executor.submit<Boolean> {
                        var isRead = false
                        // set the query params
                        val projection = arrayOf(BaseColumns._ID,
                                WikiExtractsEntry.COLUMN_TITLE,
                                WikiExtractsEntry.COLUMN_IS_READ)
                        val selection = WikiExtractsEntry.COLUMN_TITLE + " = ?"
                        val selArgs = arrayOf(wikiExtract?.title.toString())

                        val cursor = context.contentResolver.query(WikiExtractsEntry.CONTENT_URI, projection, selection,
                                selArgs, null, null)

                        // move to results
                        cursor.moveToFirst()
                        // check for empty cursor
                        if (cursor != null && cursor.count > 0) {
                            // cycle through cursor
                            isRead = 1 == cursor.getInt(cursor.getColumnIndex(WikiExtractsEntry.COLUMN_IS_READ))
                        }
                        if (!cursor.isClosed) {
                            cursor.close()
                        }
                        return@submit isRead
                    }
        } catch (e: RejectedExecutionException) {
            Logger.getLogger("WikiExtractsDao").warning(e.toString())
        }

        return try {
            future?.get(10, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            Logger.getLogger("WikiExtractsDao").warning(e.toString())
            false
        }
    }

    /**
     * Function to check whether the article loaded has been marked as read
     */
    fun setIsRead(context: Context, wikiExtract: WikiExtract?, isRead: Boolean): Boolean? {
        var future: Future<Boolean>? = null
        try {
            future =
                    WikiExtractRepository.executor.submit<Boolean> {
                        val values = ContentValues()
                        values.put(WikiExtractsEntry.COLUMN_IS_READ, isRead)

                        // Set the query params
                        val projection = arrayOf(BaseColumns._ID,
                                WikiExtractsEntry.COLUMN_TITLE)
                        val selection = WikiExtractsEntry.COLUMN_TITLE + " = ?"
                        val selArgs = arrayOf(wikiExtract?.title.toString())

                        val cursor = context.contentResolver.query(WikiExtractsEntry.CONTENT_URI, projection, selection,
                                selArgs, null, null)

                        var id = -1
                        // Move to results
                        cursor.moveToFirst()
                        // check for empty cursor
                        if (cursor != null && cursor.count > 0) {
                            id = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID))
                        }
                        if (!cursor.isClosed) {
                            cursor.close()
                        }

                        val uri = Uri.withAppendedPath(WikiExtractsEntry.CONTENT_URI, id.toString())
                        val updatedInt = context.contentResolver.update(uri, values, null, null)

                        return@submit updatedInt > 0
                    }
        } catch (e: RejectedExecutionException) {
            Logger.getLogger("WikiExtractsDao").warning(e.toString())
        }

        return try {
            future?.get(10, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            Logger.getLogger("WikiExtractsDao").warning(e.toString())
            false
        }
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
                value.put(WikiExtractsEntry.COLUMN_IS_READ, false)
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
        return context.contentResolver.bulkInsert(WikiExtractsEntry.CONTENT_URI, arrayOfValues)
    }

    /**
     * Function to grab extracts from database
     */
    fun getWikiExtracts(context: Context): MutableList<WikiExtract>? {
        // init a list of wiki extracts
        val wikiExtracts = mutableListOf<WikiExtract>()

        var future: Future<MutableList<WikiExtract>>? = null
        try {
            future =
                    WikiExtractRepository.executor.submit<MutableList<WikiExtract>> {
                        // Set the query params for the definition query.
                        val projection = arrayOf(BaseColumns._ID,
                                WikiExtractsEntry.COLUMN_PAGE_ID,
                                WikiExtractsEntry.COLUMN_TITLE,
                                WikiExtractsEntry.COLUMN_EXTRACT,
                                WikiExtractsEntry.COLUMN_THUMBNAIL)

                        val cursor = context.contentResolver.query(WikiExtractsEntry.CONTENT_URI, projection,
                                null, null, null, null)

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
        } catch (e: RejectedExecutionException) {
            Logger.getLogger("WikiExtractsDao").warning(e.toString())
        }

        return try {
            future?.get(10, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            Logger.getLogger("WikiExtractsDao").warning(e.toString())
            wikiExtracts
        }
    }

    /**
     * Delete all entries.
     */
    fun deleteYesterdayEntries(context: Context) {
        context.contentResolver.delete(WikiExtractsEntry.CONTENT_URI, null, null)
    }
}