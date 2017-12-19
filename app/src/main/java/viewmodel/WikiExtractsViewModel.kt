package viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import pojos.WikiExtract
import repository.WikiExtractRepository

/**
 * ViewModel class to keep all data related to the Wiki extracts separating the function from
 * UI controllers. The data is loaded and prepared for the UI here. The ViewModel class is
 * lifecycle aware, as well as persistent to config changes of fragments and activities.
 */
class WikiExtractsViewModel : ViewModel() {

    // LiveData holds the app data in a lifecycle conscious manner.
    lateinit var extracts: LiveData<MutableList<WikiExtract>>

    // Instance of the wiki repo to grab the function that launches the retrofit async.
    private val wikiRepo: WikiExtractRepository = WikiExtractRepository()

    /**
     * Private method handling the async load to expose data to the public getter function.
     */
    fun loadExtracts(titles: String) {
        // Handle an async for the Wiki Extracts here. Set the data returned to our LiveData object.
        extracts = wikiRepo.getExtracts(titles)
    }
}