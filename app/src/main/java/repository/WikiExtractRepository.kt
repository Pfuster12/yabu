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
class WikiExtractRepository : Callback<String> {

    companion object {
        val log: Logger? = Logger.getLogger(WikiExtractRepository::class.java.simpleName)
    }

    // Create a service instance from the companion object function.
    private val apiService by lazy { WikiAPIService.create() }

    // Variable for the LiveData object to be set in the response callback of Retrofit.
    val data: MutableLiveData<MutableList<WikiExtract>> = MutableLiveData()

    /**
     * Repo function to launch an async through the retrofit Call and return the LiveData
     * object to which the results will be set in the onResponse callbacks.
     */
    fun getExtracts(titles: String): LiveData<MutableList<WikiExtract>> {
        // Each call can make an async http request to the wiki server.
        val extractsCall: Call<String> = apiService.requestExtracts(titles)
        // Enqueue a call to an async.
        extractsCall.enqueue(this@WikiExtractRepository)
        return data
    }

    /**
     * Override function for onFailure callback of our http request.
     */
    override fun onFailure(call: Call<String>, t: Throwable?) {
        log?.warning("Http request failed")
    }

    /**
     * Override function for onResponse callback of our http request. onResponse returns a
     * Response object from okHTTP in String format through the Scalar converter passed in
     * the retrofit object for us to parse the JSON. Once parsed data is set as a wikiExtract list.
     */
    override fun onResponse(call: Call<String>, response: Response<String>?) {
        val jsonString = response?.body()
        data.value = JsonUtils.getUtils().parseJson(jsonString)
    }
}
