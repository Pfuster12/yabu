package jsondataclasses

import org.parceler.Parcel
import org.parceler.ParcelConstructor

/**
 * JSON pojos created for Moshi converter to store data from json parsing.
 */
data public class JishoKeywordJSONResponse(public val data: List<JishoWord>)

/**
 * Holds the keyword object.
 */
data public class JishoWord(public val is_common: Boolean, public val japanese: Kanji, public val sense: Sense)

/**
 * An object composed of the kanji characters and the hiragana/katakana reading
 */
@Parcel
class Kanji @ParcelConstructor constructor(public val word: String, public val reading: String) {

    // Secondary parameter for parts of speech.
    public var mParts_of_speech = ""
    public var mDefinitions = mutableListOf<String>()
    public var is_common = false
    public var jlptTag = 5
    public var url = ""
    public var isReview = false

    // Secondary constructor with parts of speech for simpler pojo before sending for a word def.
    public constructor(word: String, reading: String, partsOfSpeech: String) : this(word, reading) {
        mParts_of_speech = partsOfSpeech
    }

    // third constructor with more parts
    public constructor(word: String, reading: String, partsOfSpeech: String,
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
data public class Sense(public val english_definitions: List<String>,
                 public val parts_of_speech: String, public val tags: List<String>)