package api

import android.net.Uri
import java.net.URL

/**
 * Utility class to handle endpoint calls with WikiUtils API. The API docs can be found at
 * https://www.mediawiki.org/wiki/API:Main_page.
 * The Japanese WikiUtils API endpoint is https://ja.wikipedia.org/w/api.php. The English
 * counterpart is found at https://en.wikipedia.org/w/api.php for reference.
 */
class WikiUtils {

    // API endpoint address for Japanese Wiki API
    private val endpoint: String = "en.wikipedia.org/w/api.php"

    /**
     * Utility function to build a valid API call query to the endpoint specified above. Other
     * meta data added to the query for smoother API response.
     */
    fun queryBuilder(title: String): URL {
        // Create a URI builder.
        val builder: Uri.Builder = Uri.Builder()
        // Build the query.
        val uri: Uri = builder.scheme("https")
                .authority(endpoint)
                // Query action
                .appendQueryParameter("action", "query")
                // Extract text from sections only
                .appendQueryParameter("prop", "extracts")
                // Give response in json format
                .appendQueryParameter("format", "json")
                // Grab the intro section only
                .appendQueryParameter("exintro", "1")
                // Give extract in plain text
                .appendQueryParameter("explaintext", "1")
                // Add the title to give selected article
                .appendQueryParameter("titles", title)
                .build()
        // Create the url and return.
        return URL(uri.toString())
    }
}