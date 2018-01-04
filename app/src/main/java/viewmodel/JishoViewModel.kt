package viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import jsondataclasses.Kanji
import repository.JishoRepository
import utils.WordScanner
import java.util.logging.Logger

/**
 * ViewModel class to keep all data related to the Jisho words separating the function from
 * UI controllers. The data is loaded and prepared for the UI here. The ViewModel class is
 * lifecycle aware, as well as persistent to config changes of fragments and activities.
 */
class JishoViewModel : ViewModel() {

    // LiveData holds the app data in a lifecycle conscious manner.
    lateinit var kanjis: LiveData<MutableList<Pair<IntRange, Kanji>>>

    /**
     * Private method handling the async load to expose data to the public getter function.
     */
    fun loadKanjis(extract: String?) {
        // Build the query string for the api call
        //val queryString = WordScanner.getUtils().buildJishoQuery(extract)

        // Set the live data object
        kanjis = JishoRepository.getInstance().getWords(" 昨,日", extract)
    }
}