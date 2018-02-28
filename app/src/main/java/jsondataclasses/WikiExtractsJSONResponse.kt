package jsondataclasses

import org.parceler.Parcel
import org.parceler.ParcelConstructor

/**
 * Moshi Json parsing object of Extracts api call. Grabs a query json object containing a pages
 * array with the different extracts.
 */
data class WikiExtractsJSONResponse(public val query: WikiExtractsJSONQueryObject)

/**
 * Object holding the query. Grabs a pages object containing the list of wiki extracts.
 */
data class WikiExtractsJSONQueryObject(public val pages: List<WikiExtract>)

/**
 * Data class for the Wiki extract returned by the API call. Stores page id, title, and the extract
 * text into an object.
 */
@Parcel
data class WikiExtract @ParcelConstructor public constructor(public val pageId: Int?,
                                                             public val title: String?,
                                                             public val extract: String?,
                                                             public val thumbnail: WikiThumbnail?)

/**
 * Data class for the thumbnail object in the wiki Extract. Contains the url
 * of the image in the source keyword.
 */
@Parcel
data class WikiThumbnail @ParcelConstructor public constructor(public val source: String?)