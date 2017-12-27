package repository

import jsondataclasses.JishoKeywordJSONResponse
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Web Service class for the Jisho API returning word definitions. @GET Retrofit functions
 * take in the keyword parameter extracted from the wiki extract and send an api call to
 * get the definition.
 */
interface JishoAPIService {

    companion object {
        // API endpoint address for Wiki API
        private val endpoint: String = "http://jisho.org/"

        // companion create() fun for api service
        fun create(): JishoAPIService {
            // The Retrofit class generates an implementation of the WikiAPIService interface.
            val retrofit: Retrofit = Retrofit.Builder()
                    .baseUrl(endpoint)
                    .addConverterFactory(MoshiConverterFactory.create())
                    .build()
            return retrofit.create(JishoAPIService::class.java)
        }
    }

    /**
     * GET function of Retrofit to return word definitions from Jisho.
     * The api calls for a query of the inputted keyword. Returns a WordJSON object
     */
    @GET("api/v1/search/words?")
    fun getWords(@Query("keyword") keyword: String): Call<JishoKeywordJSONResponse>
}