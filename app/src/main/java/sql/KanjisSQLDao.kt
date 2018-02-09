package sql

import android.arch.lifecycle.MutableLiveData
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.BaseColumns
import com.yabu.android.yabu.R
import jsondataclasses.Kanji
import sql.KanjisContract.KanjisEntry
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Class to contain helper methods to save internet loaded information into the
 * database for a persistent model
 */
class KanjisSQLDao {

    companion object {
        fun getInstance(): KanjisSQLDao {
            return KanjisSQLDao()
        }
    }

    // init a mutable live data.
    val data = MutableLiveData<Kanji>()

    // init an executor for cursor calls
    val executor: ExecutorService = Executors.newCachedThreadPool()

    /**
     * Helper fun to check if there is the kanji in the database already
     * and get the id. Returns -1 if it hasn't.
     */
    fun hasDefinition(context: Context, kanji: Kanji): Pair<Int, Boolean> {
        // init kanji id in the database to -1, ie there isn't
        var id = -1
        var hasDef = false

        // Set the query params
        val projection = arrayOf(BaseColumns._ID,
                KanjisEntry.COLUMN_KANJI_WORD,
                KanjisEntry.COLUMN_DEFINITION_1)
        val selection = KanjisEntry.COLUMN_KANJI_WORD + " = ?"
        val selArgs = arrayOf(kanji.word)

        val cursor = context.contentResolver.query(KanjisEntry.CONTENT_URI, projection, selection,
                selArgs, null, null)

        // Move to results
        cursor.moveToFirst()
        // Check for empty cursor
        if (cursor != null && cursor.count > 0){
            // Cycle through cursor
            do {
                id = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID))
                // Check for each result if the def column is null
              if (!cursor.isNull(cursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_1))) {
                  hasDef = true
                  // If there is a result, get the id
                  id = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID))
              }
            } while(cursor.moveToNext())
        }
        if (!cursor.isClosed) {
            cursor.close()
        }

        // Return id and boolean
        return Pair(id, hasDef)
    }

    /**
     * Function to grab kanji with definitions from the database, among other info retrieved
     * from jisho from the single word search.
     */
    fun getKanjiDefinition(context: Context, kanji: Kanji, id: Int): Kanji {
        var kanjiFromDatabase = kanji

        val future: Future<Kanji> = executor.submit<Kanji> {
            // Set the query params for the definition query.
            val projection = arrayOf(BaseColumns._ID,
                    KanjisEntry.COLUMN_IS_COMMON,
                    KanjisEntry.COLUMN_JLPT,
                    KanjisEntry.COLUMN_DEFINITION_1,
                    KanjisEntry.COLUMN_DEFINITION_2,
                    KanjisEntry.COLUMN_URL)
            val selection = BaseColumns._ID + " = ?"
            val selArgs = arrayOf(id.toString())

            val cursor = context.contentResolver.query(KanjisEntry.CONTENT_URI, projection, selection,
                    selArgs, null, null)

            cursor.moveToFirst()
            // Check for empty cursor
            if (cursor != null && cursor.count > 0){
                // Grab all the new data
                val commonBool = cursor.getInt(cursor.getColumnIndex(KanjisEntry.COLUMN_IS_COMMON))
                val isCommon = commonBool == 1
                val jlpt = cursor.getInt(cursor.getColumnIndex(KanjisEntry.COLUMN_JLPT))
                val defs = mutableListOf<String>()
                val def1 = cursor.getString(cursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_1))
                defs.add(def1)
                // Check for a second definition
                if (!cursor.isNull(cursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_2))) {
                    val def2 = cursor.getString(cursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_2))
                    defs.add(def2)
                }
                val url = cursor.getString(cursor.getColumnIndex(KanjisEntry.COLUMN_URL))

                // Create the new kanji with data from database
                kanjiFromDatabase = Kanji(kanji.word, kanji.reading,
                        kanji.mParts_of_speech, defs, isCommon, jlpt, url)
            }
            if (!cursor.isClosed) {
                cursor.close()
            }

            return@submit kanjiFromDatabase
        }

        // return the live data object.
        return future.get()
    }

    /**
     * Dao function to insert a new kanji entry to the database.
     */
    fun saveKanji(context: Context, kanji: Pair<IntRange, Kanji>) {
        val values = ContentValues()
        values.put(KanjisContract.KanjisEntry.COLUMN_KANJI_WORD, kanji.second.word)
        values.put(KanjisContract.KanjisEntry.COLUMN_KANJI_READING, kanji.second.reading)
        values.put(KanjisContract.KanjisEntry.COLUMN_KANJI_PARTS_OF_SPEECH, kanji.second.mParts_of_speech)
        values.put(KanjisContract.KanjisEntry.COLUMN_RANGE, kanji.first.toString())

        // insert kanji into database
        context.contentResolver.insert(KanjisContract.KanjisEntry.CONTENT_URI, values)
    }

    /**
     * Dao function to update the existing kanji entry with the definitions returned
     * from a jisho call and html scrape in the Jisho Repo.
     */
    fun updateKanjiDefinition(context: Context, kanji: Kanji, id: Int): Int {
        val values = ContentValues()
        values.put(KanjisContract.KanjisEntry.COLUMN_KANJI_WORD, kanji.word)
        values.put(KanjisContract.KanjisEntry.COLUMN_IS_COMMON, kanji.is_common)
        values.put(KanjisContract.KanjisEntry.COLUMN_JLPT, kanji.jlptTag)
        values.put(KanjisContract.KanjisEntry.COLUMN_URL, kanji.url)
        if (kanji.mDefinitions.size > 0) {
            values.put(KanjisContract.KanjisEntry.COLUMN_DEFINITION_1, kanji.mDefinitions[0])
        } else {
            values.put(KanjisContract.KanjisEntry.COLUMN_DEFINITION_1, context.getString(R.string.no_definition))
        }
        if (kanji.mDefinitions.size > 1) {
            values.put(KanjisContract.KanjisEntry.COLUMN_DEFINITION_2, kanji.mDefinitions[1])
        }
        val entryUri = Uri.withAppendedPath(KanjisEntry.CONTENT_URI, id.toString())
        // update kanji entry
        //context.contentResolver.update(entryUri, values, null, null)
        return context.contentResolver.insert(KanjisEntry.CONTENT_URI, values).lastPathSegment.toInt()
    }
}