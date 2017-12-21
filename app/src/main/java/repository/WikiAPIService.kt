package repository

import jsondataclasses.WikiExtractsJSONResponse
import jsondataclasses.WikiTitlesJSONResponse
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Wiki API service interface using Retrofit library for seamless HTTP calls within Java/Kotlin.
 */
interface WikiAPIService {

    /**
     * Companion object generates an instance of WikiAPIService from Retrofit. The endpoint url
     * is fed to the retrofit object to build the query call.
     */
    companion object {
        // API endpoint address for Wiki API
        private val endpoint: String = "https://en.wikipedia.org"

        const val titleLimits = 26

        fun create(): WikiAPIService {
            // The Retrofit class generates an implementation of the WikiAPIService interface.
            val retrofit: Retrofit = Retrofit.Builder()
                    .baseUrl(endpoint)
                    .addConverterFactory(MoshiConverterFactory.create())
                    .build()
            return retrofit.create(WikiAPIService::class.java)
        }
    }

    /**
     * GET HTTP function to return Wiki extracts from url. Query parameter for the title of
     * the article is added to the function via the @Query annotation. The rest of query
     * parameters are hard-coded since they are always needed.
     *
     * The query parameter 'titles' are added in a {Title1}|{Title2}|...etc. format.
     */
    @GET("w/api.php?action=query" +
            // Retrieve extracts and thumbnail image associated with article
            "&prop=extracts|pageimages" +
            // Return extract in plain text (Boolean)
            "&explaintext=1" +
            // Return only the intro section (Boolean)
            "&exintro=1" +
            // Return the page ids as a list
            "&indexpageids" +
            // The max width of the thumbnail. As a list thumbnail 360 px will be enough (360dp)
            "&pithumbsize=360" +
            // Return in a JSON format.
            "&format=json" +
            // Return new json format with proper page array.
            "&formatversion=2")
    fun requestExtracts(@Query("titles") titles: String): Call<WikiExtractsJSONResponse>

    /**
     * GET HTTP function to return a number of featured Wikipedia link titles. These
     * link titles will be chosen at random to then be inserted into the requestExtracts()
     * function that will bring the extracts of these titles.
     */
    @GET("w/api.php?action=query" +
            // Retrieve title links
            "&prop=links" +
            // Retrieve links in the main page
            "&titles=Main_Page" +
            // Only return articles (Not users or meta pages)
            "&plnamespace=0" +
            // Return a limit number defined in companion object
            "&pllimit=" + titleLimits +
            // Return in json format
            "&format=json" +
            // Return new json format
            "&formatversion=2")
    fun requestDailyTitles(): Call<WikiTitlesJSONResponse>
}