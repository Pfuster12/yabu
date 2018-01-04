package repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import jsondataclasses.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.logging.Logger
import kotlin.math.roundToInt

/**
 * Repository class responsible for handling data operations. Mediates between different
 * data sources abstracting the logic from the ViewModel. Therefore the ViewModel does not
 * know from where the data comes from, since it does not need to. It is only concerned with
 * supplying it to the View.
 *
 * The repo implements the Retrofit Callback interface to override what happens on response
 * and on failure to the async launched by Call<T>.enqueue()
 */
class WikiExtractRepository {

    companion object {
        val log: Logger? = Logger.getLogger(WikiExtractRepository::class.java.simpleName)
        // Instance getter helper function.
        fun getInstance(): WikiExtractRepository {
            return WikiExtractRepository()
        }
    }

    // Create a service instance from the companion object function.
    private val apiService by lazy { WikiAPIService.create() }

    // Variable for the LiveData object to be set in the response callback of Retrofit.
    val data: MutableLiveData<MutableList<WikiExtract>> = MutableLiveData()

    /**
     * Repo function to launch an async through the retrofit Call and return the list of
     * daily titles to use and insert in the extract query. In onResponse the received titles
     * will be used for the getExtracts() parameter.
     */
    fun getDailyExtracts(): LiveData<MutableList<WikiExtract>> {
        // Call for the titles query json object
        val titlesCall: Call<WikiTitlesJSONResponse> = apiService.requestDailyTitles()
        // Enqueue a call to async
        titlesCall.enqueue(object : Callback<WikiTitlesJSONResponse> {

            /**
             * Override function for onResponse callback of our http request. onResponse returns a
             * Response object from okHTTP in json format through the Moshi converter passed in
             * the retrofit object for us to parse the JSON. Once parsed data is set as a wikiExtract list.
             */
            override fun onResponse(call: Call<WikiTitlesJSONResponse>,
                                    response: Response<WikiTitlesJSONResponse>?) {
                // Build the titles query.
                val titleQuery = buildTitlesQuery(response?.body())
                // With the query titles built, send the call to get the extracts
                getExtracts(titleQuery)
            }

            /**
             * Override function for onFailure callback of our http request.
             */
            override fun onFailure(call: Call<WikiTitlesJSONResponse>, t: Throwable?) {
                log?.warning("Titles Http request failed")
            }
        })
        // Return the live data object holding the list set by the getExtracts() onResponse.
        return data
    }

    /**
     * Repo function to launch an async through the retrofit Call and return the LiveData
     * object to which the results will be set in the onResponse callbacks.
     */
    fun getExtracts(titles: String) {
        // Call for the extracts query json
        val extractsCall: Call<WikiExtractsJSONResponse> = apiService.requestExtracts(titles)
        // Callback implementation
        // Enqueue a call to an async.
        extractsCall.enqueue(object : Callback<WikiExtractsJSONResponse> {

            /**
             * Override function for onResponse callback of our http request. onResponse returns a
             * Response object from okHTTP in String format through the Scalar converter passed in
             * the retrofit object for us to parse the JSON. Once parsed data is set as a wikiExtract list.
             */
            override fun onResponse(call: Call<WikiExtractsJSONResponse>,
                                    response: Response<WikiExtractsJSONResponse>?) {
                // Grab the json response
                val jsonResponse = response?.body()
                // Set the value of the mutable live data to the return parsed list.
                data.value = jsonResponse?.query?.pages?.toMutableList()
            }

            /**
             * Override function for onFailure callback of our http request.
             */
            override fun onFailure(call: Call<WikiExtractsJSONResponse>, t: Throwable?) {
                log?.warning("Extracts Http request failed")
            }
        })
    }

    /**
     * Helper fun to filter list out from number titles and Wikipedia meta pages.
     */
    private fun filterExtractList(titlesArg: List<WikiTitles>): List<WikiTitles> {
        // Set to a var
        var titles = titlesArg

        // Check if there are titles since safe null boolean is not allowed for
        // filter() predicate.
        if (titles.any()) {

            // Loop to filter every title starting with a number
            for (i in 0..9) {
                // Filter list to take out any extracts starting with a number
                titles = titles.filterNot {
                    it -> it.title!!.startsWith(i.toString(), true) }
            }

            // Filter to take out any extracts starting with an english character
            titles = titles.filterNot {
                it -> it.title!!.startsWith("w", true) }
        }

        // Return list
        return titles
    }

    /**
     * Helper fun to build the title query string
     */
    private fun buildTitlesQuery(jsonResponse: WikiTitlesJSONResponse?): String {
        // init the title string empty.
        var buildTitleQuery = ""
        if (jsonResponse != null) {
            // Grab the container with the links of titles.
            var titles: List<WikiTitles> = jsonResponse.query.pages[0].links
            // Filter the list to contain only proper titles.
            titles = filterExtractList(titles)

            // init a starting index.
            val start = 10
            // math op to find the range to get ~10 items, no matter the size of
            //  the list. Decimal results are rounded to the nearest int.
            val range: Int = (titles.size - start).div(10).toDouble().roundToInt()

            // Construct the title string with chosen start and step.
            for (i in start until titles.size step range) {
                // Grab the current title.
                val currentTitle = titles[i].title
                // build the title string by adding '...|{Title}'
                buildTitleQuery =
                        if (i == start) buildTitleQuery.plus(currentTitle)
                        else buildTitleQuery.plus("|" + currentTitle)
            }
        }

        // Return the query string
        return buildTitleQuery
    }
}
