package repository

import android.arch.lifecycle.MutableLiveData
import jsondataclasses.Kanji
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import utils.WordScanner
import java.util.logging.Logger

/**
 * Repo class to retrieve data from the Jisho.org API service. The repo will provide
 * to the viewmodel the definitions from each kanji found in the text.
 */
class JishoRepository {

    companion object {
        val log: Logger? = Logger.getLogger(WikiExtractRepository::class.java.simpleName)
        // Get an instance helper function
        fun getInstance(): JishoRepository {
            return JishoRepository()
        }
    }

    // Create a jisho service instance
    private val apiService by lazy { JishoAPIService.create() }

    // Variable for the LiveData Jisho Word to be set in the response callback of Retrofit.
    val data: MutableLiveData<MutableList<Pair<IntRange, Kanji>>> = MutableLiveData()

    /**
     * Repo fun to retrieve the word definitions from Jisho as retrieved from the extract
     * texts by the word scanner.
     */
    fun getWords(words: String?, extract: String?): MutableLiveData<MutableList<Pair<IntRange, Kanji>>> {
        // Get the Call for the html string
        val jishoCall: Call<String> = apiService.getWordsFromJisho(words)

        val pairs = WordScanner.getUtils().scanText(extract)

        // Enqueue a call to async
        jishoCall.enqueue(object : Callback<String> {
            /**
             * Override function for onResponse callback of our http request. onResponse returns
             * a Response object from okHTTP in String format of the html doc. Once parsed data
             * is set as a Jisho Keyword.
             */
            override fun onResponse(call: Call<String>?, response: Response<String>?) {
                // Grab the response body which is the string of the html.
                val htmlString = response?.body()

                // Grab the kanji pojos from the html string by scraping with jsoup
                val kanjis = parseHtmlForFurigana(htmlString)

                log?.warning(kanjis[0].word)

                // Combine the extract int range
                val pairIndexKanji = combineKanjisAndIndex(pairs, kanjis)

                // Set the LiveData value to the html string.
                data.value = pairIndexKanji
            }

            /**
             * Override function for onFailure callback of our http request.
             */
            override fun onFailure(call: Call<String>?, t: Throwable?) {
                log?.warning("Jisho Http request failed.")
            }
        })

        // Return the LiveData object that is updated when onResponse is called
        return data
    }

    /**
     * Helper fun to scrape the jisho html for the word definitions and furigana using
     * the Jsoup library.
     */
    private fun parseHtmlForFurigana(html: String?): MutableList<Kanji> {
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

        // Return the list
        return kanjis
    }

    /**
     * Helper fun to combine the Kanjis from the html to the int ranges.
     */
    fun combineKanjisAndIndex(pairs: MutableList<Pair<IntRange, String>>,
                              kanjis: MutableList<Kanji>): MutableList<Pair<IntRange, Kanji>> {
        // Init the index and kanji pairs
        val kanjiIndexPairs = mutableListOf<Pair<IntRange, Kanji>>()

        // Start a while loop.
        var i = 0
        while (i < kanjis.size) {
            // Grab the current pair
            val currentPair = pairs[i]
            // Grab the current kanji
            val currentKanji = kanjis[i]

            // Put the current int range and the current kanji together into the new list
            kanjiIndexPairs.add(Pair(currentPair.first, currentKanji))

            // Increment i
            i++
        }

        // Return the kanji-index pairs
        return kanjiIndexPairs
    }
}