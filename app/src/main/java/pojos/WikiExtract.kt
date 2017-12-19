package pojos

/**
 * POJO for the Wiki extract returned by the API call. Stores page id, title, and the extract
 * text into an object.
 */
data class WikiExtract constructor(val pageId: Int?,
                              val titleExtract: String?,
                              val textExtract: String?) {
}// Parameters of the json result to hold data.