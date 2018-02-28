package jsondataclasses

/**
 * Moshi Json parsing object of Titles api call. Grabs a query json object containing a pages
 * object with the links property exposing the link titles in the Main Page.
 */
public data class WikiTitlesJSONResponse(public val query: WikiTitlesJSONQueryObject)

/**
 * Object of query key in titles json response. Grabs a pages json object
 * containing the object with the links property exposing the titles in the Main Page.
 */
public data class WikiTitlesJSONQueryObject(public val pages: List<WikiTitlesJSONContainer>)

/**
 * Object of page json response. Grabs a list of wiki titles json objects
 * containing the name of the link titles.
 */
public data class WikiTitlesJSONContainer(public val links: List<WikiTitles>)

/**
 * Data class containing the name of the titles returned by the call.
 */
public data class WikiTitles(public val title: String?)