package repository

import pojos.WikiExtract
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Wiki API service interface using Retrofit library for seamless HTTP calls within Java/Kotlin.
 */
interface WikiAPIService {

    companion object {

        // API endpoint address for Japanese Wiki API
        val endpoint: String = "en.wikipedia.org/"

        fun create(): WikiAPIService {
            // The Retrofit class generates an implementation of the WikiAPIService interface.
            val retrofit: Retrofit = Retrofit.Builder()
                    .baseUrl(endpoint)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            return retrofit.create(WikiAPIService::class.java)
        }
    }

    /**
     * GET HTTP function to return Wiki extracts from url. Query parameter for the title of
     * the article is added to the function via the @Query annotation. The rest of query
     * parameters are hard-coded since they are always needed.
     */

    @GET("w/api.php?action=query&prop=extracts&explaintext=1&exintro=1&format=json")
    fun requestExtracts(@Query("titles") titles: String): Call<WikiExtract>
}