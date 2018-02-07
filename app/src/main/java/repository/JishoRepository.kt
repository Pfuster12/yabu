package repository

import android.arch.lifecycle.MutableLiveData
import jsondataclasses.Kanji
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.logging.Logger

/**
 * Repo class to retrieve data from the Jisho.org API service. The repo will provide
 * to the viewmodel the definitions from each kanji found in the text.
 */
class JishoRepository {

    companion object {
        val log: Logger? = Logger.getLogger(JishoRepository::class.java.simpleName)
        // Get an instance helper function
        fun getInstance(): JishoRepository {
            return JishoRepository()
        }
    }

    // Create a jisho service instance
    private val apiService by lazy { JishoAPIService.create() }

    // Variable for the LiveData Kanji pair to be set in the response callback of Retrofit.
    val data: MutableLiveData<MutableList<Pair<IntRange, Kanji>>> = MutableLiveData()

    // Variable for the LiveData Kanji returned from Jisho
    val wordData: MutableLiveData<Kanji> = MutableLiveData()

    /**
     * Repo fun to retrieve the word furigana from Jisho as retrieved from the extract
     * texts by the word scanner.
     */
    fun getWords(words: String?, extract: String?): MutableLiveData<MutableList<Pair<IntRange, Kanji>>> {
        // Get the Call for the html string
        val jishoCall: Call<String> = apiService.getWordsFromJisho(words)

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

                // Set the LiveData value to the html string.
                data.value = combineKanjisAndIndex(extract, kanjis)
            }

            /**
             * Override function for onFailure callback of our http request.
             */
            override fun onFailure(call: Call<String>?, t: Throwable?) {
                log?.warning("Jisho Furigana Http request failed.")
            }
        })

        // Return the LiveData object that is updated when onResponse is called
        return data
    }

    /**
     * Repo fun to retrieve the definition of a word from Jisho as touched by a user to show
     * the callout bubble definition.
     */
    fun getDefinitions(protoKanji: Kanji): MutableLiveData<Kanji> {
        var keyword: String = protoKanji.word
        // Check if it is one char to add the #kanji metatag
        when (protoKanji.word.length == 1) {
            true -> keyword = protoKanji.word + "#kanji"
        }

        val url = "http://jisho.org/search/" + keyword
        // Get the call for the html string
        val jishoCall: Call<String> = apiService.getDefinitionFromJisho(keyword)

        // Enqueue a call to async
        jishoCall.enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>?, response: Response<String>?) {
                // Grab the response body which is the string of the html.
                val htmlString = response?.body()

                // Parse html for definitions
                wordData.value = parseHtmlForDefinitions(protoKanji, htmlString, url)
            }

            override fun onFailure(call: Call<String>?, t: Throwable?) {
                log?.warning("Jisho Definition Http request failed.")
            }

        })

        return wordData
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
                meaningList, isCommon, jlptid, url)
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
}