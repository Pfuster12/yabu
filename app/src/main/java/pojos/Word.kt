package pojos

/**
 * POJO for a Word retrieved from the text. A Word consists of the word character, its reading,
 * a common boolean tag, definitions and the type of word it is, i.e. noun, verb, adjective.
 */
data class Word constructor(val word: String?,
                            val reading: String?,
                            val common: Boolean?,
                            val definitions: MutableList<String>?,
                            val type: String?)