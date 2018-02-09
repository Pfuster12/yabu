package viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import jsondataclasses.Kanji
import repository.JishoRepository
import utils.WordScanner

/**
 * ViewModel class to keep all data related to the Jisho words separating the function from
 * UI controllers. The data is loaded and prepared for the UI here. The ViewModel class is
 * lifecycle aware, as well as persistent to config changes of fragments and activities.
 */
class JishoViewModel : ViewModel() {

    // LiveData holds the app data in a lifecycle conscious manner.
    lateinit var kanjis: LiveData<MutableList<Pair<IntRange, Kanji>>>

    lateinit var kanji: LiveData<Kanji>

    private val jishoRepo = JishoRepository.getInstance()

    /**
     * Method handling the async load to expose kanji and furigana data to the ui that calls it.
     */
    fun loadKanjis(extract: String?) {
        // Build the query string for the api call
        val queryString = WordScanner.getUtils().buildJishoQuery(extract)

        // Set the live data object
        kanjis = jishoRepo.getWords(queryString, extract)
    }

    /**
     * Method handling the async load to expose kanji definitions and tags to the ui that calls it.
     */
    fun getDefinitions(context: Context, protoKanji: Kanji) {
        kanji = jishoRepo.getDefinitions(context, protoKanji)
    }

}