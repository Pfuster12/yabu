package repository

/**
 * Utility class to handle endpoint calls with JsonUtils API. The API docs can be found at
 * https://www.mediawiki.org/wiki/API:Main_page.
 * The Japanese JsonUtils API endpoint is https://ja.wikipedia.org/w/api.php. The English
 * counterpart is found at https://en.wikipedia.org/w/api.php for reference.
 */
class JsonUtils {

    /**
     * Companion object to initialize the utils and access functions.
     */
    companion object {

        // Number of extracts to grab from the api call.
        val extractNumber = 5

        /**
         * Helper function to init the utils object.
         */
        fun getUtils(): JsonUtils {
            return JsonUtils()
        }
    }

 /*   *//**
     * Utility function to parse the json string returned by a http call of the wiki endpoint.
     * The json must be manually parsed because the wiki api is formatted as json objects of
     * each page id, which are different for each article. Therefore it can't be deserialized
     * into pojos as they don't have the same key for each call.
     *//*
    fun parseJson(jsonString: String?): MutableList<WikiExtract> {
        // Top-level wikiExtract mutable list to be added to in parsing
        val wikiExtracts: MutableList<WikiExtract> = mutableListOf()

        // Convert the string into the container json object
        val containerJson = JSONObject(jsonString)
        // Grab the query json object
        val query = containerJson.optJSONObject("query")
        // Grab the pageids array to know the pages objects names.
        val pageIds = query.optJSONArray("pageids")
        // Grab the pages json object
        val pages = query.optJSONObject("pages")

        // Begin a while loop to iterate through each page. We don't know the page key, that is
        // why the pageids array is called to grab the id key.
        var i = 0
        while (i < pageIds.length()) {
            // Grab the current page object
            val page = pages.optJSONObject(pageIds[i].toString())
            val pageid = page.optInt("pageid")
            val title = page.optString("title")
            val extract = page.optString("extract")
            // Grab the thumbnail json object
            val thumbnailjson = page.optJSONObject("thumbnail")
            // Grab the source url of the thumbnail
            val thumbnail = thumbnailjson.optString("source")
            // Create a new wikiExtract pojo
            val wikiExtract = WikiExtract(pageid, title, extract, thumbnail)
            wikiExtracts.add(wikiExtract)
            i++
        }
        // Return the mutable list with added extracts.
        return wikiExtracts
    }*/
}