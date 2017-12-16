package pojos

/**
 * POJO for the Wiki extract returned by the API call. Stores page id, title, and the extract
 * text into an object.
 */
class WikiExtract constructor(val pageId: Int, val titleExtract: String, val textExtract: String) {
}