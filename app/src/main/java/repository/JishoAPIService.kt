package repository

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

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
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build()

            return retrofit.create(JishoAPIService::class.java)
        }
    }

    /**
     * GET function of Retrofit to return word furigana from Jisho.
     * The api calls for a query of the inputted keyword. Returns a string of html to scrape.
     */
    @GET("search/{keyword}")
    fun getWordsFromJisho(@Path("keyword") keyword: String?): Call<String>

    /**
     * GET function of Retrofit to return word definitions from Jisho.
     * The api calls for a query hof the inputted keyword. Returns a string of html to scrape.
     */
    @GET("search/{keyword}")
    fun getDefinitionFromJisho(@Path("keyword") keyword: String?): Call<String>
}