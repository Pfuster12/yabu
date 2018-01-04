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
    var parts_of_speech = ""

    // Secondary constructor with parts of speech for simpler pojo before sending for a word def.
    constructor(word: String, reading: String, partsOfSpeech: String) : this(word, reading) {
        parts_of_speech = partsOfSpeech
    }
}

/**
 * An object composed of the eng definitions, the parts of speech and any tags applied
 * by Jisho e.g. "usually written in kana alone".
 */
data class Sense(val english_definitions: List<String>,
                 val parts_of_speech: String, val tags: List<String>)