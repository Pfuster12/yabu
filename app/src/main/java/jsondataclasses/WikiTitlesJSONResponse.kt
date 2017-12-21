package jsondataclasses

/**
 * Moshi Json parsing object of Titles api call. Grabs a query json object containing a pages
 * object with the links property exposing the link titles in the Main Page.
 */
data class WikiTitlesJSONResponse(val query: WikiTitlesJSONQueryObject)

/**
 * Object of query key in titles json response. Grabs a pages json object
 * containing the object with the links property exposing the titles in the Main Page.
 */
data class WikiTitlesJSONQueryObject(val pages: List<WikiTitlesJSONContainer>)

/**
 * Object of page json response. Grabs a list of wiki titles json objects
 * containing the name of the link titles.
 */
data class WikiTitlesJSONContainer(val links: List<WikiTitles>)

/**
 * Data class containing the name of the titles returned by the call.
 */
data class WikiTitles(val title: String?)