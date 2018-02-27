package widget

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.provider.BaseColumns
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.yabu.android.yabu.R
import org.jsoup.Connection
import sql.KanjisContract.KanjisEntry

/**
 * List View remote views implementation
 */
class ListViewWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent?): RemoteViewsFactory {
        return ListRemoteViewsFactory(this.applicationContext)
    }

    class ListRemoteViewsFactory(val context: Context) : RemoteViewsService.RemoteViewsFactory {

        private lateinit var mCursor: Cursor

        override fun onCreate() {
            // Empty
        }

        override fun getLoadingView(): RemoteViews {
            return RemoteViews(context.packageName, R.layout.callout_bubble_loading)
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun onDataSetChanged() {
            // query the cursor to get new data
            // Set the query params for the definition query.
            val projection = arrayOf(BaseColumns._ID,
                    KanjisEntry.COLUMN_KANJI_WORD,
                    KanjisEntry.COLUMN_KANJI_READING,
                    KanjisEntry.COLUMN_KANJI_PARTS_OF_SPEECH,
                    KanjisEntry.COLUMN_DEFINITION_1,
                    KanjisEntry.COLUMN_DEFINITION_2,
                    KanjisEntry.COLUMN_IS_COMMON,
                    KanjisEntry.COLUMN_JLPT,
                    KanjisEntry.COLUMN_URL,
                    KanjisEntry.COLUMN_IS_REVIEW)
            val selection = KanjisEntry.COLUMN_IS_REVIEW + " = ?"
            val selArgs = arrayOf(1.toString())

            mCursor = context.contentResolver.query(KanjisEntry.CONTENT_URI, projection, selection,
                    selArgs, null, null)
        }

        override fun hasStableIds(): Boolean {
            return true
        }

        override fun getViewAt(position: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_item)
            mCursor.moveToPosition(position)

            val word =
                    mCursor.getString(mCursor.getColumnIndex(KanjisEntry.COLUMN_KANJI_WORD))
            val reading =
                    mCursor.getString(mCursor.getColumnIndex(KanjisEntry.COLUMN_KANJI_READING))
            val partsOfSpeech =
                    mCursor.getString(mCursor.getColumnIndex(KanjisEntry.COLUMN_KANJI_PARTS_OF_SPEECH))

            var isCommon = false
            // Grab all the new data
            if (!mCursor.isNull(mCursor.getColumnIndex(KanjisEntry.COLUMN_IS_COMMON))) {
                val commonBool = mCursor.getInt(mCursor.getColumnIndex(KanjisEntry.COLUMN_IS_COMMON))
                isCommon = commonBool == 1
            }
            var jlpt = -1
            if (!mCursor.isNull(mCursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_1))) {
                jlpt = mCursor.getInt(mCursor.getColumnIndex(KanjisEntry.COLUMN_JLPT))
            }
            // init a definition list
            val defs = mutableListOf<String>()
            if (!mCursor.isNull(mCursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_1))) {
                val def1 = mCursor.getString(mCursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_1))
                defs.add(def1)
            }
            // Check for a second definition
            if (!mCursor.isNull(mCursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_2))) {
                val def2 = mCursor.getString(mCursor.getColumnIndex(KanjisEntry.COLUMN_DEFINITION_2))
                defs.add(def2)
            }
            var url = ""
            // Check for a second definition
            if (!mCursor.isNull(mCursor.getColumnIndex(KanjisEntry.COLUMN_URL))) {
                url = mCursor.getString(mCursor.getColumnIndex(KanjisEntry.COLUMN_URL))
            }

            // Set title.
            views.setTextViewText(R.id.widget_review_title, word)
            // Set text.
            views.setTextViewText(R.id.widget_review_reading, reading)
            // set the definitions
            when (defs.size) {
                0 -> {
                    views.setTextViewText(R.id.widget_review_definition_1,
                            context.getString(R.string.no_definition))
                    views.setViewVisibility(R.id.widget_review_definition_2, View.GONE)
                }
                1 -> {
                    if (defs[0] == context.getString(R.string.no_definition)) {
                        views.setTextViewText(R.id.widget_review_definition_1,
                                context.getString(R.string.no_definition))
                    } else {
                        views.setTextViewText(R.id.widget_review_definition_1, context.getString(R.string.definition_1_placeholder, defs[0]))
                    }
                    views.setViewVisibility(R.id.widget_review_definition_2, View.GONE)
                }
                2 -> {
                    views.setViewVisibility(R.id.widget_review_definition_2, View.VISIBLE)
                    views.setTextViewText(R.id.widget_review_definition_1, context.getString(R.string.definition_1_placeholder, defs[0]))
                    views.setTextViewText(R.id.widget_review_definition_2, context.getString(R.string.definition_2_placeholder, defs[1]))
                }
            }

            // Check for the common tag
            if (isCommon) {
                views.setViewVisibility(R.id.widget_review_common_tag, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_review_common_tag, View.GONE)
            }

            // Check the jlpt level and set color and text
            when (jlpt) {
                -1 -> views.setViewVisibility(R.id.widget_review_jlpt_tag, View.GONE)
                1 -> {
                    views.setViewVisibility(R.id.widget_review_jlpt_tag, View.VISIBLE)
                    views.setTextViewText(R.id.widget_review_jlpt_tag, context.getString(R.string.JLPTN1))
                }
                2 -> {
                    views.setViewVisibility(R.id.widget_review_jlpt_tag, View.VISIBLE)
                    views.setTextViewText(R.id.widget_review_jlpt_tag, context.getString(R.string.JLPTN2))
                }
                3 -> {
                    views.setViewVisibility(R.id.widget_review_jlpt_tag, View.VISIBLE)
                    views.setTextViewText(R.id.widget_review_jlpt_tag, context.getString(R.string.JLPTN3))
                }
                4 -> {
                    views.setViewVisibility(R.id.widget_review_jlpt_tag, View.VISIBLE)
                    views.setTextViewText(R.id.widget_review_jlpt_tag, context.getString(R.string.JLPTN4))
                }
                5 -> {
                    views.setViewVisibility(R.id.widget_review_jlpt_tag, View.VISIBLE)
                    views.setTextViewText(R.id.widget_review_jlpt_tag, context.getString(R.string.JLPTN5))
                }
            }

            if (!url.isEmpty()) {
                // if there is jisho data show details link
                views.setViewVisibility(R.id.widget_review_details_link, View.VISIBLE)
                // make fill intent with url to load web app
                val fillInIntent = Intent()
                fillInIntent.putExtra(YabuWidget.URL_EXTRA, url)
                // Set a listener to know when the alpha ends to return to 1.0f alpha
                views.setOnClickFillInIntent(R.id.widget_review_details_link, fillInIntent)
            } else {
                // if there is jisho data show details link
                views.setViewVisibility(R.id.widget_review_details_link, View.INVISIBLE)
            }

            return views
        }

        override fun getCount(): Int {
            return mCursor.count
        }

        override fun getViewTypeCount(): Int {
            return 1
        }

        override fun onDestroy() {
            mCursor.close()
        }
    }
}