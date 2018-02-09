package sql

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import sql.WikiExtractsContract.WikiExtractsEntry

/**
 * SQLite database creator helper.
 */
class WikiExtractsDbHelper(context: Context) : SQLiteOpenHelper(context, "wikiextracts.db", null, 1) {

    private val SQL_CREATE_ENTRIES = "CREATE TABLE " + WikiExtractsEntry.TABLE_NAME + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY," +
            WikiExtractsEntry.COLUMN_PAGE_ID + " INTEGER NOT NULL," +
            WikiExtractsEntry.COLUMN_DATE + " INTEGER," +
            WikiExtractsEntry.COLUMN_TITLE + " TEXT NOT NULL," +
            WikiExtractsEntry.COLUMN_EXTRACT + " TEXT," +
            WikiExtractsEntry.COLUMN_THUMBNAIL + " TEXT)"

    /*
  ----- ID ----   WIKI TITLE    ----  WIKI EXTRACT   --- etc..
  ------------------------------------------------------------
  -----  1 ----     eg.         ----      blabal     --- etc..
  -----  2 ----    ex..         ----    kqkweoidk5   --- etc..
   */

    private val DROP_TABLE = "DROP TABLE IF EXISTS " + WikiExtractsEntry.TABLE_NAME

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Drop table as it is just a cache.
        db?.execSQL(DROP_TABLE)
        onCreate(db)
    }
}