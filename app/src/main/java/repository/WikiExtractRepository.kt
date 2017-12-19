package repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import pojos.WikiExtract
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.logging.Logger

/**
 * Repository class responsible for handling data operations. Mediates between different
 * data sources abstracting the logic from the ViewModel. Therefore the ViewModel does not
 * know from where the data comes from, since it does not need to. It is only concerned with
 * supplying it to the View.
 *
 * The repo implements the Retrofit Callback interface to override what happens on response
 * and on failure to the async launched by Call<T>.enqueue()
 */
class WikiExtractRepository : Callback<WikiExtract> {

    companion object {
        val log = Logger.getLogger(WikiExtractRepository::class.java.simpleName)
    }

    // Create a service instance from the companion object function.
    val apiService by lazy { WikiAPIService.create() }

    val data: MutableLiveData<WikiExtract> = MutableLiveData()

    fun getExtracts(titles: String): LiveData<WikiExtract> {
        // Each call can make an async http request to the wiki server.
        val extractsCall: Call<WikiExtract> = apiService.requestExtracts(titles)
        extractsCall.enqueue(this@WikiExtractRepository)
        return data
    }

    /**
     * Override function for onFailure callback of our http request.
     */
    override fun onFailure(call: Call<WikiExtract>?, t: Throwable?) {
        log.warning("Http request failed")
    }

    /**
     * Override function for onResponse callback of our http request.
     */
    override fun onResponse(call: Call<WikiExtract>?, response: Response<WikiExtract>?) {
        data.value = response?.body()
    }
}
