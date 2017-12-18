package api

import pojos.WikiExtract
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Wiki API service interface using Retrofit library for seamless HTTP calls within Java/Kotlin.
 */
interface WikiAPIService {

    @GET("w/api.php?action=query&prop=extracts&explaintext=1&exintro=1&format=json")
    fun extracts(@Path("user")title: String,
                 @Query("titles") titles: String): Call<MutableList<WikiExtract>>
}