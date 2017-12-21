package jsondataclasses

import org.parceler.Parcel
import org.parceler.ParcelConstructor

/**
 * Moshi Json parsing object of Extracts api call. Grabs a query json object containing a pages
 * array with the different extracts.
 */
data class WikiExtractsJSONResponse(val query: WikiExtractsJSONQueryObject)

/**
 * Object holding the query. Grabs a pages object containing the list of wiki extracts.
 */
data class WikiExtractsJSONQueryObject(val pages: List<WikiExtract>)

/**
 * Data class for the Wiki extract returned by the API call. Stores page id, title, and the extract
 * text into an object.
 */
@Parcel
data class WikiExtract @ParcelConstructor constructor(val pageId: Int?,
                                   val title: String?,
                                   val extract: String?,
                                   val thumbnail: WikiThumbnail?) // Parameters of the json result to hold data.

/**
 * Data class for the thumbnail object in the wiki Extract. Contains the url
 * of the image in the source keyword.
 */
@Parcel
data class WikiThumbnail @ParcelConstructor constructor(val source: String?)