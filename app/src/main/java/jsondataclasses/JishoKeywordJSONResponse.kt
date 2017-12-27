package jsondataclasses

/**
 * JSON pojos created for Moshi converter to store data from json parsing.
 */
data class JishoKeywordJSONResponse(val data: List<JishoKeyword>)

/**
 * Holds the keyword object.
 */
data class JishoKeyword(val is_common: Boolean, val japanese: List<Kanji>, val senses: List<Sense>)

/**
 * An object composed of the kanji characters and the hiragana/katakana reading
 */
data class Kanji(val word: String, val reading: String)

/**
 * An object composed of the eng definitions, the parts of speech and any tags applied
 * by Jisho e.g. "usually written in kana alone".
 */
data class Sense(val english_definitions: List<String>,
                 val parts_of_speech: List<String>, val tags: List<String>)