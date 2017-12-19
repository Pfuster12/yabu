package repository

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
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

        fun create(): WikiAPIService {
            // The Retrofit class generates an implementation of the WikiAPIService interface.
            val retrofit: Retrofit = Retrofit.Builder()
                    .baseUrl(endpoint)
                    .addConverterFactory(ScalarsConverterFactory.create())
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

    @GET("w/api.php?action=query&prop=extracts&explaintext=1&exintro=1&indexpageids&format=json")
    fun requestExtracts(@Query("titles") titles: String): Call<String>
}