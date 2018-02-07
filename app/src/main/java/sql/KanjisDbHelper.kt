package sql

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import sql.KanjisContract.KanjisEntry

/**
 * SQLite database creator helper.
 */
class KanjisDbHelper(context: Context) : SQLiteOpenHelper(context, "favorites.db", null, 1) {

    val SQL_CREATE_ENTRIES = "CREATE TABLE " + KanjisEntry.TABLE_NAME + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY," +
            KanjisEntry.COLUMN_KANJI_WORD + " TEXT NOT NULL," +
            KanjisEntry.COLUMN_KANJI_READING + " TEXT," +
            KanjisEntry.COLUMN_KANJI_PARTS_OF_SPEECH + " TEXT," +
            // Boolean values are 0 (false) and 1 (true).
            KanjisEntry.COLUMN_IS_COMMON + " INTEGER," +
            KanjisEntry.COLUMN_JLPT + " TEXT," +
            KanjisEntry.COLUMN_DEFINITION_1 + " TEXT," +
            KanjisEntry.COLUMN_DEFINITION_2 + " TEXT," +
            KanjisEntry.COLUMN_URL + " TEXT)"

    /*
  ----- ID ----    KANJI WORD   ----  KANJI READING  --- etc..
  ------------------------------------------------------------
  -----  1 ----     eg.         ----      blabal     --- etc..
  -----  2 ----    ex..         ----    kqkweoidk5   --- etc..
   */

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Do nothing.
    }
}