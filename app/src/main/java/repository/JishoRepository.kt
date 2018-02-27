package repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.net.ConnectivityManager
import jsondataclasses.Kanji
import jsondataclasses.WikiExtract
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import sql.KanjisSQLDao
import utils.WordScanner
import java.util.concurrent.*
import java.util.logging.Logger

/**
 * Repo class to retrieve data from the Jisho.org API service. The repo will provide
 * to the viewmodel the definitions from each kanji found in the text.
 */
class JishoRepository {

    companion object {
        // logger
        val log: Logger? = Logger.getLogger(JishoRepository::class.java.simpleName)

        // Executor variable to execute in worker threads
        lateinit var executor: ExecutorService

        // Get an instance helper function
        fun getInstance(): JishoRepository {
            executor = Executors.newCachedThreadPool()
            return JishoRepository()
        }
    }

    // Create a jisho service instance
    private val apiService by lazy { JishoAPIService.create() }

    // Variable for the LiveData Kanji pair to be set in the response callback of Retrofit.
    val readingData: MutableLiveData<MutableList<Pair<IntRange, Kanji>>> = MutableLiveData()

    // Variable for the LiveData Kanji pair to be set in the response callback of Retrofit.
    val wordData: MutableLiveData<Pair<Int?, Kanji?>> = MutableLiveData()

    // SQL dao instance
    private val kanjiDao = KanjisSQLDao.getInstance()

    /**
     * Repo fun to retrieve the word furigana from Jisho as retrieved from the extract
     * texts by the word scanner.
     */
    fun getWords(context: Context, wikiExtract: WikiExtract?): MutableLiveData<MutableList<Pair<IntRange, Kanji>>> {
        refreshWords(context, wikiExtract)

        readingData.value = kanjiDao.getReadings(context, wikiExtract?.title!!)
        // Return the LiveData object that is updated when onResponse is called
        return readingData
    }

    private fun refreshWords(context: Context, wikiExtract: WikiExtract?): Boolean? {
        var future: Future<Boolean>? = null
        // launch a worker thread
        try {
            future = executor.submit<Boolean> {

                // check if words are from yesterday
                val isToday = kanjiDao.isTodayWords(context)
                if (!isToday) {
                    kanjiDao.deleteYesterdayKanjis(context)
                }
                // check for furigana readings
                val hasReadings = kanjiDao.hasReadings(context, wikiExtract?.title)

                if (!hasReadings && isConnected(context)) {
                    // Build the query string for the api call
                    val words = WordScanner.getUtils().buildJishoQuery(wikiExtract?.extract)
                    // If there aren't, execute a web call
                    val response = apiService.getWordsFromJisho(words).execute()
                    if (response.isSuccessful) {
                        // Grab the response body which is the string of the html.
                        val htmlString = response?.body()

                        // Grab the kanji pojos from the html string by scraping with jsoup
                        val kanjis = parseHtmlForFurigana(htmlString)

                        // get the index and kanji pairs in the text
                        val kanjiPairs = combineKanjisAndIndex(wikiExtract?.extract, kanjis)

                        // save in database the pairs
                        kanjiDao.saveKanjiReadings(context, kanjiPairs, wikiExtract?.title!!)
                    } else {
                        log?.warning("Jisho Furigana Http request failed.")
                    }
                }

                return@submit hasReadings
            }
        } catch (e: RejectedExecutionException) {
            Logger.getLogger("JishoRepo").warning(e.toString())
        }

        return try {
            future?.get(10, TimeUnit.SECONDS)
        } catch (e: Exception) {
            Logger.getLogger("JishoRepo").warning(e.toString())
            false
        }
    }

    /**
     * Repo fun to retrieve the definition of a word from Jisho as touched by a user to show
     * the callout bubble definition.
     */
    fun getDefinitions(context: Context, protoKanji: Kanji): LiveData<Pair<Int?, Kanji?>> {
        // see if there is an existing definition, if not do a retrofit call
        val id = refreshDefinitions(context, protoKanji)

        wordData.value = Pair(id, kanjiDao.getKanjiDefinition(context, protoKanji, id!!))
        // return a live data directly from the database
        return wordData
    }

    /**
     * Helper function to get definitions if there isn't any.
     */
    private fun refreshDefinitions(context: Context, protoKanji: Kanji): Int? {
        var keyword: String = protoKanji.word

        // Check if it is one char to add the #kanji meta-tag
        when (protoKanji.word.length == 1) {
            true -> keyword = protoKanji.word + "#kanji"
        }
        // build the url
        val url = "http://jisho.org/search/" + keyword

        var future: Future<Int>? = null

        try {
            future = executor.submit<Int> {
                // running in a background thread
                // Check if word has definition
                val id = kanjiDao.hasDefinition(context, protoKanji)

                // Check to see if there isn't a definition
                if (!id.second && isConnected(context)) {
                    // refresh the data
                    // execute the call, and check for error
                    val response = apiService.getDefinitionFromJisho(keyword).execute()
                    if (response.isSuccessful) {
                        // Update the database. The live data will automatically refresh.
                        // Grab the response body which is the string of the html.
                        val htmlString = response.body()

                        // Parse html for definitions
                        val kanji = parseHtmlForDefinitions(protoKanji, htmlString, url)

                        kanjiDao.updateKanjiDefinition(context, kanji, id.first)
                    } else {
                        // Error in web call.
                        log?.warning("Jisho definition Http request failed.")
                    }
                }

                return@submit id.first
            }
        } catch (e: RejectedExecutionException) {
            Logger.getLogger("JishoRepo").warning(e.toString())
        }

        return try {
            future?.get(10, TimeUnit.SECONDS)
        } catch (e: Exception) {
            Logger.getLogger("JishoRepo").warning(e.toString())
           -1
        }
    }

    /**
     * Helper fun to scrape the jisho html for the word definitions and furigana using
     * the Jsoup library.
     */
    private fun parseHtmlForFurigana(html: String?): List<Kanji> {
        // init a list of Kanji pojos
        val kanjis = mutableListOf<Kanji>()

        // Parse the string response from the Retrofit call into a Jsoup html doc.
        val jsoup: Document = Jsoup.parse(html)

        // Grab the content of the word and furigana with the id zen_bar
        val content: Element = jsoup.getElementById("zen_bar")
        // Grab the class with each word as a list.
        val wordClass = content.getElementsByClass("clearfix japanese_word")

        // Iterate through the words class to grab the furigana and the kanji data of each word.
        for (word in wordClass) {
            // Grab the part of speech
            val partsOfSpeech = word.attr("data-pos").toString()
            // Grab the kanji char
            val kanji = word
                    .getElementsByClass("japanese_word__text_with_furigana")
                    .text()
            // Grab the furigana chars
            val furigana = word
                    .getElementsByClass("japanese_word__furigana")
                    .text()

            // Create the Kanji pojo
            val kanjiPojo = Kanji(kanji, furigana, partsOfSpeech)
            // Add to the list.
            kanjis.add(kanjiPojo)
        }

        // Filter the list
        // Return the list
        return kanjis.filterNot { kanji -> kanji.word.isEmpty() }

    }

    private fun parseHtmlForDefinitions(protoKanji: Kanji, html: String?, url: String): Kanji {
        // Boolean to see if common tag is present
        var isCommon = false
        var jlptTag = ""
        var jlptid = -1
        val meaningList = mutableListOf<String>()

        // Parse the string response from the Retrofit call into a Jsoup html doc.
        val jsoup: Document = Jsoup.parse(html)

        if (protoKanji.word.length == 1) {
            // Grab the content of the exact word definition div
            val content: Elements = jsoup.getElementsByClass("kanji details")

            if (content.size != 0) {
                // Grab the kanji meaning element
                val definitions = content[0].getElementsByClass("kanji-details__main-meanings")

                if (definitions.size != 0) {
                    meaningList.add(definitions[0].text().trim())
                }

                val stats = content[0].getElementsByClass("kanji_stats")

                if (stats.size != 0) {
                    val jlptElement = stats[0].getElementsByClass("jlpt")

                    if (jlptElement.size != 0) {
                        val jlptText = jlptElement[0].text()

                        when (jlptText) {
                            "JLPT level N1" -> jlptid = 1
                            "JLPT level N2" -> jlptid = 2
                            "JLPT level N3" -> jlptid = 3
                            "JLPT level N4" -> jlptid = 4
                            "JLPT level N5" -> jlptid = 5
                        }
                    }
                }
            }
        } else {
            // Grab the content of the exact word definition div
            val content: Elements = jsoup.getElementsByClass("exact_block")

            if (content.size != 0) {
                // Grab the tags element
                val tags = content[0].getElementsByClass("concept_light-status")

                // Check if there are tags
                if (tags.size != 0) {
                    // Grab the common tag
                    val common = tags[0].getElementsByClass("concept_light-tag concept_light-common success label")

                    // Check if there is a common tag
                    if (common.size != 0) {
                        isCommon = true
                    }

                    // Grab the JLPT tag
                    val jlptElement = tags[0].getElementsByClass("concept_light-tag label")

                    if (jlptElement.size != 0) {
                        jlptTag = jlptElement[0].data()

                        when (jlptTag) {
                            "JLPT N1" -> jlptid = 1
                            "JLPT N2" -> jlptid = 2
                            "JLPT N3" -> jlptid = 3
                            "JLPT N4" -> jlptid = 4
                            "JLPT N5" -> jlptid = 5
                        }
                    }
                }

                // Grab the definitions
                val meanings = content[0].getElementsByClass("meaning-meaning")
                // Add each meaning to the definition list.
                meanings.forEach { meaning -> meaningList.add(meaning.text().trim()) }
            }
        }

        // Return a kanji with definitions and tags.
        return Kanji(protoKanji.word, protoKanji.reading, protoKanji.mParts_of_speech,
                meaningList, isCommon, jlptid, url, false)
    }

    /**
     * Helper fun to combine the Kanjis from the html to the int ranges.
     */
    fun combineKanjisAndIndex(extract: String?,
                              kanjis: List<Kanji>): MutableList<Pair<IntRange, Kanji>> {
        // Init the index and kanji pairs
        val kanjiIndexPairs = mutableListOf<Pair<IntRange, Kanji>>()

        // Start a while loop with start index.
        var startIndex = 0
        var i = 0
        while (i < kanjis.size) {
            // Grab the index of the kanji in the extract text.
            val index1: Int = extract!!.indexOf(kanjis[i].word, startIndex, true)

            // To get the int range, add the size of the kanji chars to index1
            val index2 = index1 + (kanjis[i].word.length - 1)
            // Create the Int Range
            val intRange = IntRange(index1, index2)

            // Add the pair to the list
            kanjiIndexPairs.add(Pair(intRange, kanjis[i]))
            // Increment i
            i++
            // Make startIndex the index of the kanji in the extract since the list is ascending.
            startIndex = index1 + 1
        }


        // Return the kanji-index pairs
        return kanjiIndexPairs
    }

    /**
     * Helper fun to check internet connection whether wi-fi or mobile
     */
    private fun isConnected(context: Context): Boolean {
        val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val networkInfo = connMgr?.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
}