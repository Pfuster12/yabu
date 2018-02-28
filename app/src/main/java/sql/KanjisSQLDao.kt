package sql

import android.arch.lifecycle.MutableLiveData
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.BaseColumns
import com.yabu.android.yabu.R
import com.yabu.android.yabu.ui.MainActivity
import com.yabu.android.yabu.ui.ReviewWordsFragment
import jsondataclasses.Kanji
import jsondataclasses.WikiExtract
import repository.JishoRepository
import sql.KanjisContract.KanjisEntry
import java.util.concurrent.*
import java.util.logging.Logger

/**
 * Class to contain helper methods to save internet loaded information into the
 * kanjis database for a persistent model
 */
class KanjisSQLDao {

    companion object {
        fun getInstance(): KanjisSQLDao {
            return KanjisSQLDao()
        }
    }

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
            } while (cursor.moveToNext())
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
    fun getKanjiDefinition(context: Context, kanji: Kanji, id: Int?): Kanji? {
        var kanjiFromDatabase = kanji

        var future: Future<Kanji>? = null

        try {
            future = JishoRepository.executor.submit<Kanji> {
                // Set the query params for the definition query.
                val projection = arrayOf(BaseColumns._ID,
                        KanjisEntry.COLUMN_IS_COMMON,
                        KanjisEntry.COLUMN_JLPT,
                        KanjisEntry.COLUMN_DEFINITION_1,
                        KanjisEntry.COLUMN_DEFINITION_2,
                        KanjisEntry.COLUMN_URL,
                        KanjisEntry.COLUMN_IS_REVIEW)
                val selection = BaseColumns._ID + " = ?"
                val selArgs = arrayOf(id.toString())

                val cursor = context.contentResolver.query(KanjisEntry.CONTENT_URI, projection, selection,
                        selArgs, null, null)

                cursor.moveToFirst()
                // Check for empty cursor
                if (cursor != null && cursor.count > 0){
                    var isCommon = false
                    // Grab all the new data
                    if (!cursor.isNull(cursor.getColumnIndex(KanjisEntry.COLUMN_IS_COMMON))) {
                        val commonBool = cursor.getInt(cursor.getColumnIndex(KanjisEntry.COLUMN_IS_COMMON))
                        isCommon = commonBool == 1
                    }
                    var jlpt = -1
                    if (!cursor.isNull(cursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_1))) {
                        jlpt = cursor.getInt(cursor.getColumnIndex(KanjisEntry.COLUMN_JLPT))
                    }
                    // init a definition list
                    val defs = mutableListOf<String>()
                    if (!cursor.isNull(cursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_1))) {
                        val def1 = cursor.getString(cursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_1))
                        defs.add(def1)
                    }
                    // Check for a second definition
                    if (!cursor.isNull(cursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_2))) {
                        val def2 = cursor.getString(cursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_2))
                        defs.add(def2)
                    }
                    var url = ""
                    // Check for a second definition
                    if (!cursor.isNull(cursor.getColumnIndex(KanjisEntry.COLUMN_URL))) {
                        url = cursor.getString(cursor.getColumnIndex(KanjisEntry.COLUMN_URL))
                    }

                    val isReviewBool = cursor.getInt(cursor.getColumnIndex(KanjisEntry.COLUMN_IS_REVIEW))
                    val isReview = isReviewBool == 1

                    // Create the new kanji with data from database
                    kanjiFromDatabase = Kanji(kanji.word, kanji.reading,
                            kanji.mParts_of_speech, defs, isCommon, jlpt, url, isReview)
                }
                if (!cursor.isClosed) {
                    cursor.close()
                }

                return@submit kanjiFromDatabase
            }
        } catch (e: RejectedExecutionException) {
            Logger.getLogger("KanjiSQLDao").warning(e.toString())
        }

        // return the live data object.
        return try {
            future?.get(10, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            Logger.getLogger("KanjiSQLDao").warning(e.toString())
            kanjiFromDatabase
        }
    }


    /**
     * Dao fun to check if a wiki extract kanji has readings in the database already
     */
    fun isTodayWords(context: Context): Boolean {
        var date = -1
        // get todays day
        val today = java.util.Calendar.getInstance().get(java.util.Calendar.DATE)
        // Set the query params
        val projection = arrayOf(BaseColumns._ID,
                KanjisEntry.COLUMN_DATE)

        val selection = KanjisEntry.COLUMN_DATE + " = ?"

        val cursor = context.contentResolver.query(KanjisEntry.CONTENT_URI, projection, selection,
                null, null, null)

        // Move to results
        cursor.moveToFirst()
        // Check for empty cursor
        if (cursor != null && cursor.count > 0) {
            date = cursor.getInt(cursor.getColumnIndex(KanjisEntry.COLUMN_DATE))
        }
        val isTodayBool = date == today
        // close cursor
        if (!cursor.isClosed) {
            cursor.close()
        }

        // Return id and boolean
        return isTodayBool
    }

    /**
     * Dao fun to check if a wiki extract kanji has readings in the database already
     */
    fun hasReadings(context: Context, wikiTitle: String?): Boolean {
        // Set the query params
        val projection = arrayOf(BaseColumns._ID,
                KanjisEntry.COLUMN_SOURCE)
        val selection = KanjisEntry.COLUMN_SOURCE + " = ?"
        val selArgs = arrayOf(wikiTitle)

        val cursor = context.contentResolver.query(KanjisEntry.CONTENT_URI, projection, selection,
                selArgs, null, null)

        // Move to results
        cursor.moveToFirst()
        // Check for empty cursor
        val hasReadingsBool = (cursor != null && cursor.count > 0)
        // close cursor
        if (!cursor.isClosed) {
            cursor.close()
        }

        // Return id and boolean
        return hasReadingsBool
    }

    fun getReadings(context: Context, wikiTitle: String): MutableList<Pair<IntRange, Kanji>>? {
        val kanjiPairs = mutableListOf<Pair<IntRange, Kanji>>()

        var future: Future<MutableList<Pair<IntRange, Kanji>>>? = null

        try {
            future =
                    JishoRepository.executor.submit<MutableList<Pair<IntRange, Kanji>>> {
                        // Set the query params for the definition query.
                        val projection = arrayOf(BaseColumns._ID,
                                KanjisEntry.COLUMN_SOURCE,
                                KanjisEntry.COLUMN_KANJI_WORD,
                                KanjisEntry.COLUMN_KANJI_READING,
                                KanjisEntry.COLUMN_KANJI_PARTS_OF_SPEECH,
                                KanjisEntry.COLUMN_RANGE)
                        val selection = KanjisEntry.COLUMN_SOURCE + " = ?"
                        val selArgs = arrayOf(wikiTitle)

                        val cursor = context.contentResolver.query(KanjisEntry.CONTENT_URI, projection, selection,
                                selArgs, null, null)

                        cursor.moveToFirst()
                        // Check for empty cursor
                        if (cursor != null && cursor.count > 0){
                            do {
                                val word =
                                        cursor.getString(cursor.getColumnIndex(KanjisEntry.COLUMN_KANJI_WORD))
                                val reading =
                                        cursor.getString(cursor.getColumnIndex(KanjisEntry.COLUMN_KANJI_READING))
                                val partsOfSpeech =
                                        cursor.getString(cursor.getColumnIndex(KanjisEntry.COLUMN_SOURCE))
                                val range =
                                        cursor.getString(cursor.getColumnIndex(KanjisEntry.COLUMN_RANGE))
                                // split the range in two
                                val ranges = range.split("..")

                                // Create the new kanji with data from database
                                val kanji = Kanji(word, reading, partsOfSpeech)
                                val intRange = IntRange(ranges[0].toInt(), ranges[1].toInt())
                                // add to list
                                kanjiPairs.add(Pair(intRange, kanji))
                            } while (cursor.moveToNext())
                        }

                        // close the cursor
                        if (!cursor.isClosed) {
                            cursor.close()
                        }

                        return@submit kanjiPairs
                    }
        } catch (e: RejectedExecutionException) {
            Logger.getLogger("KanjiSQLDao").warning(e.toString())
        }

        // return the live data object.
        return try {
            future?.get(10, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            Logger.getLogger("KanjiSQLDao").warning(e.toString())
            kanjiPairs
        }
    }

    /**
     * Dao function to insert a new kanji entry to the database.
     */
    fun saveKanjiReadings(context: Context, kanjiPairs: MutableList<Pair<IntRange, Kanji>>, wikiTitle: String) {
        // get todays day
        val today = java.util.Calendar.getInstance().get(java.util.Calendar.DATE)

        val values = mutableListOf<ContentValues>()
        for (kanji in kanjiPairs) {
            val value = ContentValues()
            value.put(KanjisEntry.COLUMN_DATE, today)
            value.put(KanjisEntry.COLUMN_SOURCE, wikiTitle)
            value.put(KanjisEntry.COLUMN_KANJI_WORD, kanji.second.word)
            value.put(KanjisEntry.COLUMN_KANJI_READING, kanji.second.reading)
            value.put(KanjisEntry.COLUMN_KANJI_PARTS_OF_SPEECH, kanji.second.mParts_of_speech)
            value.put(KanjisEntry.COLUMN_RANGE, kanji.first.toString())
            value.put(KanjisEntry.COLUMN_IS_REVIEW, false)

            // put the value in the list
            values.add(value)
        }

        // insert kanji into database
        context.contentResolver.bulkInsert(KanjisEntry.CONTENT_URI, values.toTypedArray())
    }

    /**
     * Dao function to update the existing kanji entry with the definitions returned
     * from a jisho call and html scrape in the Jisho Repo.
     */
    fun updateKanjiDefinition(context: Context, kanji: Kanji, id: Int) {
        val values = ContentValues()
        values.put(KanjisEntry.COLUMN_KANJI_WORD, kanji.word)
        values.put(KanjisEntry.COLUMN_IS_COMMON, kanji.is_common)
        values.put(KanjisEntry.COLUMN_JLPT, kanji.jlptTag)
        values.put(KanjisEntry.COLUMN_URL, kanji.url)
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
        context.contentResolver.update(entryUri, values, null, null)
    }

    /**
     * Dao function to get all the kanji with the review boolean set to true in the
     * favorite button.
     */
    fun getReviewKanjis(context: Context): MutableList<Pair<Int, Kanji>>? {
        val reviewWords = mutableListOf<Pair<Int, Kanji>>()

        var future: Future<MutableList<Pair<Int, Kanji>>>? = null

        // launch a worker thread to query the review words
        try {
            future =
                    MainActivity.executor.submit<MutableList<Pair<Int, Kanji>>> {

                        // Set the query params for the definition query.
                        val projection = arrayOf(BaseColumns._ID,
                                KanjisEntry.COLUMN_KANJI_WORD,
                                KanjisEntry.COLUMN_KANJI_READING,
                                KanjisEntry.COLUMN_KANJI_PARTS_OF_SPEECH,
                                KanjisEntry.COLUMN_DEFINITION_1,
                                KanjisEntry.COLUMN_DEFINITION_2,
                                KanjisEntry.COLUMN_IS_COMMON,
                                KanjisEntry.COLUMN_JLPT,
                                KanjisEntry.COLUMN_URL,
                                KanjisEntry.COLUMN_IS_REVIEW)
                        val selection = KanjisEntry.COLUMN_IS_REVIEW + " = ?"
                        val selArgs = arrayOf(1.toString())

                        val cursor = context.contentResolver.query(KanjisEntry.CONTENT_URI, projection, selection,
                                selArgs, null, null)

                        cursor.moveToFirst()
                        // Check for empty cursor
                        if (cursor != null && cursor.count > 0){
                            do {
                                val word =
                                        cursor.getString(cursor.getColumnIndex(KanjisEntry.COLUMN_KANJI_WORD))
                                val reading =
                                        cursor.getString(cursor.getColumnIndex(KanjisEntry.COLUMN_KANJI_READING))
                                val partsOfSpeech =
                                        cursor.getString(cursor.getColumnIndex(KanjisEntry.COLUMN_KANJI_PARTS_OF_SPEECH))

                                var isCommon = false
                                // Grab all the new data
                                if (!cursor.isNull(cursor.getColumnIndex(KanjisEntry.COLUMN_IS_COMMON))) {
                                    val commonBool = cursor.getInt(cursor.getColumnIndex(KanjisEntry.COLUMN_IS_COMMON))
                                    isCommon = commonBool == 1
                                }
                                var jlpt = -1
                                if (!cursor.isNull(cursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_1))) {
                                    jlpt = cursor.getInt(cursor.getColumnIndex(KanjisEntry.COLUMN_JLPT))
                                }
                                // init a definition list
                                val defs = mutableListOf<String>()
                                if (!cursor.isNull(cursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_1))) {
                                    val def1 = cursor.getString(cursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_1))
                                    defs.add(def1)
                                }
                                // Check for a second definition
                                if (!cursor.isNull(cursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_2))) {
                                    val def2 = cursor.getString(cursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_2))
                                    defs.add(def2)
                                }
                                var url = ""
                                // Check for a second definition
                                if (!cursor.isNull(cursor.getColumnIndex(KanjisEntry.COLUMN_URL))) {
                                    url = cursor.getString(cursor.getColumnIndex(KanjisEntry.COLUMN_URL))
                                }

                                val isReviewBool = cursor.getInt(cursor.getColumnIndex(KanjisEntry.COLUMN_IS_COMMON))
                                val isReview = isReviewBool == 1

                                // Create the new kanji with data from database
                                val reviewKanji = Kanji(word, reading,
                                        partsOfSpeech, defs, isCommon, jlpt, url, isReview)

                                val entryId = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID))

                                reviewWords.add(Pair(entryId, reviewKanji))
                            } while (cursor.moveToNext())
                        }

                        // close the cursor
                        if (!cursor.isClosed) {
                            cursor.close()
                        }

                        return@submit reviewWords
                    }
        } catch (e: RejectedExecutionException) {
            Logger.getLogger("KanjiSQLDao").warning(e.toString())
        }

        // return the live data object.
        return try {
            future?.get(10, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            Logger.getLogger("KanjiSQLDao").warning(e.toString())
            reviewWords
        }
    }

    /**
     * Dao function to update the review boolean in the table. This will make the word
     * part of the review list.
     */
    fun updateReviewKanji(context: Context, id: Int?, isReview: Boolean): Int? {
        var future: Future<Int>? = null

        try {
            future = MainActivity.executor.submit<Int> {
                val values = ContentValues()
                values.put(KanjisEntry.COLUMN_IS_REVIEW, isReview)

                val entryUri = Uri.withAppendedPath(KanjisEntry.CONTENT_URI, id.toString())
                // update kanji entry
                return@submit context.contentResolver.update(entryUri, values, null, null)
            }
        } catch (e: RejectedExecutionException) {
            Logger.getLogger("KanjiSQLDao").warning(e.toString())
        }

        return try {
            future?.get(10, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            Logger.getLogger("KanjiSQLDao").warning(e.toString())
            -1
        }
    }

    /**
     * Dao function to delete all non review kanjis
     */
    fun deleteYesterdayKanjis(context: Context) {
        val whereClause = KanjisEntry.COLUMN_IS_REVIEW + " = ?"
        val whereArgs = arrayOf(0.toString())

        context.contentResolver.delete(KanjisEntry.CONTENT_URI, whereClause, whereArgs)
    }
}