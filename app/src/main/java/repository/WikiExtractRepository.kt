package repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.net.ConnectivityManager
import jsondataclasses.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import sql.WikiExtractsSQLDao
import java.util.concurrent.*
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

        // Executor variable to execute in worker threads
        lateinit var executor: ExecutorService
        // Instance getter helper function.
        fun getInstance(): WikiExtractRepository {
            executor = Executors.newCachedThreadPool()
            return WikiExtractRepository()
        }
    }

    // Create a service instance from the companion object function.
    private val apiService by lazy { WikiAPIService.create() }

    // Variable for the LiveData object to be set in the response callback of Retrofit.
    val data: MutableLiveData<MutableList<WikiExtract>> = MutableLiveData()

    private val wikiDao = WikiExtractsSQLDao.getInstance()

    /**
     * Repo function to launch an async through the retrofit Call and return the list of
     * daily titles to use and insert in the extract query. In onResponse the received titles
     * will be used for the getExtracts() parameter.
     */
    fun getDailyExtracts(context: Context): LiveData<MutableList<WikiExtract>> {
        // check if extracts are old and need to be refreshed
        refreshExtracts(context)

        data.value = wikiDao.getWikiExtracts(context)
        // Return the live data object holding the list set by the getExtracts() onResponse.
        return data
    }

    private fun refreshExtracts(context: Context): Boolean? {
        var future: Future<Boolean>? = null
        // execute worker thread
        try {
            future = executor.submit<Boolean> {
                var isSaved = false
                // running in a background thread
                // Check to see if the extracts need to refresh to the daily articles
                val isToday = wikiDao.isToday(context)

                if (!isToday && isConnected(context)) {
                    // refresh the data
                    // execute the call, and check for error
                    val response = apiService.requestDailyTitles().execute()
                    if (response.isSuccessful) {
                        // Update the database. The live data will automatically refresh.
                        // Grab the response which is the Build the titles query.
                        val titleQuery = buildTitlesQuery(response?.body())

                        // With the query titles built, send the call to get the extracts
                        isSaved = saveExtracts(context, titleQuery)
                    } else {
                        // Error in web call.
                        log?.warning("Titles Http request failed")
                    }
                }
                return@submit isSaved
            }
        } catch (e: RejectedExecutionException) {
            Logger.getLogger("JishoRepo").warning(e.toString())

        }

        return try {
            future?.get(10, TimeUnit.SECONDS)
        } catch (e: Exception) {
            Logger.getLogger("JishoRepo").warning(e.toString())
            false
        }
    }

    /**
     * Repo function to launch an async through the retrofit Call and return the LiveData
     * object to which the results will be set in the onResponse callbacks.
     */
    private fun saveExtracts(context: Context, titles: String): Boolean {
        var rows = -1
        // execute the extracts query json call
        val response = apiService.requestExtracts(titles).execute()
        if (response.isSuccessful) {
            // delete entries only if there is internet connection
            wikiDao.deleteYesterdayEntries(context)
            // Grab the json response
            val jsonResponse = response?.body()
            // save the wiki extracts into the database
            rows = wikiDao.saveWikiExtracts(context, jsonResponse?.query?.pages)
        } else {
            log?.warning("Extracts Http request failed")
        }

        return rows != -1
    }

    fun isRead(context: Context, wikiExtract: WikiExtract?): Boolean? {
        return wikiDao.isRead(context, wikiExtract)
    }

    fun setRead(context: Context, wikiExtract: WikiExtract?, isRead: Boolean) {
        wikiDao.setIsRead(context, wikiExtract, isRead)
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

    /**
     * Helper fun to check internet connection whether wi-fi or mobile
     */
    private fun isConnected(context: Context): Boolean {
        val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val networkInfo = connMgr?.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
}
