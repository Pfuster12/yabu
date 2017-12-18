package data

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import pojos.WikiExtract

/**
 * ViewModel class to keep all data related to the Wiki extracts separating the function from
 * UI controllers. The data is loaded and prepared for the UI here. The ViewModel class is
 * lifecycle aware, as well as persistent to config changes of fragments and activities.
 */
class WikiExtractsViewModel : ViewModel() {

    // Mutable LiveData holds the app data in a lifecycle conscious manner.
    var extracts: MutableLiveData<Array<WikiExtract>> = MutableLiveData()

    /**
     * Private method handling the async load to expose data to the public getter function.
     */
    fun loadExtracts() {
        // Handle an async for the Wiki Extracts here.
        // Test array
        val extract1 = WikiExtract(0,
                "J.R.R. Tolkien", "Tolkien was a cool guy who knew how to write.")
        val extract2 = WikiExtract(1,
                "Bath", "Bath is a pretty city and you better be there.")
        val extract3 = WikiExtract(2,
                "Money", "I have no money right now.")
        val array: Array<WikiExtract> = arrayOf(extract1, extract2, extract3)
        // Set LiveData value to the test array.
        extracts.value = array
    }
}