package sql

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import jsondataclasses.WikiExtract
import sql.WikiExtractsContract.WikiExtractsEntry

/**
 * Content Provider to access the Wiki extracts database. The provider gives a second layer of
 * abstraction and safety to access the database.
 */
class WikiExtractsContentProvider : ContentProvider() {
    // Global db helper var
    private lateinit var mDbHelper: WikiExtractsDbHelper

    private val sUriMatcher = UriMatcher(UriMatcher.NO_MATCH)
    private val WIKI_ALL_ID = 100
    private val WIKI_SINGLE_ID = 101

    // Add the Uri for all the table and a single Id to the matcher
    private fun setUriMatcher() {
        sUriMatcher.addURI(WikiExtractsContract.CONTENT_AUTHORITY,
                WikiExtractsEntry.TABLE_NAME, WIKI_ALL_ID)
        sUriMatcher.addURI(WikiExtractsContract.CONTENT_AUTHORITY,
                WikiExtractsEntry.TABLE_NAME + "/#", WIKI_SINGLE_ID)
    }

    /**
     * Insert function.
     */
    override fun insert(uri: Uri?, values: ContentValues?): Uri {
        val database = mDbHelper.writableDatabase
        var id: Long = 0

        when (sUriMatcher.match(uri)) {
            WIKI_ALL_ID -> {
                id = database.insert(WikiExtractsEntry.TABLE_NAME, null, values)
            }
            WIKI_SINGLE_ID -> {
                // Do nothing
            }
            else -> throw IllegalArgumentException("Uri not recognised")
        }

        return Uri.withAppendedPath(uri, id.toString())
    }

    /**
     * bulk insert function
     */
    override fun bulkInsert(uri: Uri?, values: Array<out ContentValues>?): Int {
        val database = mDbHelper.writableDatabase

        // begin a transaction for a bulk insert.
        database.beginTransaction()
        var rows = 0
        try {
            // Loop for every wiki extract
            for (value in values!!) {
                // insert into database
                database.insert(WikiExtractsEntry.TABLE_NAME, null, value)
                rows++
            }
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }

        return rows
    }

    /**
     * Query function for the content provider. Takes in all columns to query
     */
    override fun query(uri: Uri?, projection: Array<out String>?, selection: String?,
                       selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        // Get a readable db to query
        val database = mDbHelper.readableDatabase
        val cursor: Cursor

        // Match the uri to find which corresponds to
        when (sUriMatcher.match(uri)) {
        // All the entries
            WIKI_ALL_ID -> {
                cursor = database.query(WikiExtractsEntry.TABLE_NAME, projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        null)
            }
        // One entry
            WIKI_SINGLE_ID -> {
                cursor = database.query(WikiExtractsEntry.TABLE_NAME, projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        null)
            }
            else -> throw IllegalArgumentException("Uri not recognised")
        }

        return cursor
    }

    /**
     * Instantiate a new db helper database. Set the uri matchers.
     */
    override fun onCreate(): Boolean {
        setUriMatcher()
        mDbHelper = WikiExtractsDbHelper(context)
        return true
    }

    /**
     * Update function for a single entry
     */
    override fun update(uri: Uri?, values: ContentValues?, selection: String?,
                        selectionArgs: Array<out String>?): Int {
        val database = mDbHelper.writableDatabase
        var id = 0

        when (sUriMatcher.match(uri)) {
            WIKI_ALL_ID -> {
                // Do nothing
            }
            WIKI_SINGLE_ID -> {
                val whereClause = BaseColumns._ID + " = ?"
                val whereArgs = arrayOf(uri?.lastPathSegment)

                id = database.update(WikiExtractsEntry.TABLE_NAME, values, whereClause, whereArgs)
            }
        }

        return id
    }

    /**
     * Delete function for a single entry or the whole table.
     */
    override fun delete(uri: Uri?, selection: String?, selectionArgs: Array<out String>?): Int {
        val database = mDbHelper.writableDatabase
        var id = 0

        when (sUriMatcher.match(uri)) {
            WIKI_ALL_ID -> {
                id = database.delete(WikiExtractsEntry.TABLE_NAME, selection, null)
            }
            WIKI_SINGLE_ID -> {
                val sel = BaseColumns._ID + " = ?"
                val selArgs = arrayOf(uri?.lastPathSegment)

                // Delete the entry
                id = database.delete(WikiExtractsEntry.TABLE_NAME, sel, selArgs)
            }
            else -> throw IllegalArgumentException("Uri not recognised")
        }

        return id
    }

    /**
     * Function to get vnd type of each uri.
     */
    override fun getType(uri: Uri?): String {
        when (sUriMatcher.match(uri)) {
            WIKI_ALL_ID -> {
                return "vnd.android.cursor.dir/" +
                        "vnd.com.example.KanjisContentProvider." +
                        WikiExtractsEntry.TABLE_NAME
            }
            WIKI_SINGLE_ID -> {
                return "vnd.android.cursor.item/" +
                        "vnd.com.example.KanjisContentProvider." +
                        WikiExtractsEntry.TABLE_NAME
            }
            else -> throw IllegalArgumentException("Uri not recognised")
        }
    }
}