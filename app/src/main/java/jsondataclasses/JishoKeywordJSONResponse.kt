package jsondataclasses

/**
 * JSON pojos created for Moshi converter to store data from json parsing.
 */
data class JishoKeywordJSONResponse(val data: List<JishoWord>)

/**
 * Holds the keyword object.
 */
data class JishoWord(val is_common: Boolean, val japanese: Kanji, val sense: Sense)

/**
 * An object composed of the kanji characters and the hiragana/katakana reading
 */
class Kanji(val word: String, val reading: String) {

    // Secondary parameter for parts of speech.
    var mParts_of_speech = ""
    var mDefinitions = mutableListOf<String>()
    var is_common = false
    var jlptTag = 5
    var url = ""
    var isReview = false

    // Secondary constructor with parts of speech for simpler pojo before sending for a word def.
    constructor(word: String, reading: String, partsOfSpeech: String) : this(word, reading) {
        mParts_of_speech = partsOfSpeech
    }

    // Secondary constructor with parts of speech for simpler pojo before sending for a word def.
    constructor(word: String, reading: String, partsOfSpeech: String,
                definitions: MutableList<String>,
                isCommon: Boolean, jlpt: Int,
                linkUrl: String, isReviewBool: Boolean) : this(word, reading, partsOfSpeech) {
        mDefinitions = definitions
        is_common = isCommon
        jlptTag = jlpt
        url = linkUrl
        isReview = isReviewBool
    }
}

/**
 * An object composed of the eng definitions, the parts of speech and any tags applied
 * by Jisho e.g. "usually written in kana alone".
 */
data class Sense(val english_definitions: List<String>,
                 val parts_of_speech: String, val tags: List<String>)