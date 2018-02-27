package viewmodel

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import jsondataclasses.Kanji
import sql.KanjisSQLDao

/**
 * Review Words fragment view model. Takes care of querying the database for review words
 * from the Kanjis DAO object.
 */
class ReviewViewModel : ViewModel() {

    // LiveData holds the app data in a lifecycle conscious manner.
    val reviewKanjis: MutableLiveData<MutableList<Pair<Int, Kanji>>> = MutableLiveData()

    val kanjisDao = KanjisSQLDao.getInstance()

    /**
     * Method handling the async load to expose kanji and furigana data to the ui that calls it.
     */
    fun loadReviewKanjis(context: Context) {
        // Set the live data object
        reviewKanjis.value = kanjisDao.getReviewKanjis(context)
    }
}