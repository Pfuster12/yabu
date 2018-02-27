package com.yabu.android.yabu.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.yabu.android.yabu.R
import jsondataclasses.Kanji
import android.widget.ImageView
import kotlinx.android.synthetic.main.fragment_review_words.view.*
import sql.KanjisSQLDao

/**
 * Recycler View custom adapter for review words with a header,
 * footer and item view holder. Passes the adapter data, the activity context
 * and the listener callback to the activity.
 */
class ReviewRecyclerViewAdapter(private val reviewKanjis: MutableList<Pair<Int, Kanji>>,
                                private val context: Context, private val listener: (reviewKanji: Pair<Int, Kanji>) -> Unit,
                                private val notifyListener: (isEmpty: Boolean) -> Unit, private val fragmentRootView: View)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /*
   Types of view holders to return for the function itemViewType
    */
    private val headerType = 100
    private val itemType = 101
    private val footerType = 102

    // init a Kanji Dao
    val kanjiDao = KanjisSQLDao.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
        // Start a return when condition with the type int.
        return when (viewType) {
        // If header type inflate the header resource.
            headerType -> ReviewRecyclerViewAdapter.HeaderViewHolder(
                    inflateViewHolder(R.layout.reading_heading, parent))
        // If item type inflate the item resource.
            itemType -> ReviewRecyclerViewAdapter.ReviewWordItemViewHolder(
                    inflateViewHolder(R.layout.review_list_item, parent), reviewKanjis, listener)
        // If footer type inflate the footer resource.
            footerType -> ReviewRecyclerViewAdapter.FooterViewHolder(
                    inflateViewHolder(R.layout.review_footer, parent))
        // Else return the item layout as a default.
            else -> ReviewRecyclerViewAdapter.ReviewWordItemViewHolder(
                    inflateViewHolder(R.layout.reading_list_item, parent), reviewKanjis, listener)
        }
    }

    /**
     * Helper function to inflate a layout resource.
     */
    private fun inflateViewHolder(layoutId: Int, parent: ViewGroup?): View {
        return LayoutInflater.from(context).inflate(layoutId, parent, false)
    }

    override fun getItemCount(): Int {
        // Return the lists size with header and footer.
        return reviewKanjis.size + 1 + 1
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        // Start a when condition with the view type int.
        when (getItemViewType(position)) {
        // If header type then bind header vectors
            headerType -> {} // Do bind operations here
        // If item type then bind texts and images with wikiExtracts pojo.
            itemType -> {
                // Cast the holder to a reading item.
                val itemViewHolder = holder as? ReviewRecyclerViewAdapter.ReviewWordItemViewHolder?
                // Bind the views.
                bindListItemHolder(itemViewHolder, position)
            }
        // If footer type bind footer texts and vectors.
            footerType -> {}// Do bind operations here
        }
    }

    /**
     * Helper function to bind extract properties to the view holder.
     */
    private fun bindListItemHolder(itemViewHolder: ReviewRecyclerViewAdapter.ReviewWordItemViewHolder?,
                                   position: Int) {
        // Position is minus 1 because of the header
        val currentPosition = position - 1
        // Extract current extract
        val currentReviewKanjiPair = reviewKanjis[currentPosition]
        // Set the divider
        itemViewHolder?.divider?.visibility = View.VISIBLE
        // Set title.
        itemViewHolder?.word?.text = currentReviewKanjiPair.second.word
        // Set text.
        itemViewHolder?.reading?.text = currentReviewKanjiPair.second.reading
        // set the definitions
        when (currentReviewKanjiPair.second.mDefinitions.size) {
            0 -> {
                itemViewHolder?.definition1?.text = context.getString(R.string.no_definition)
                itemViewHolder?.definition2?.visibility = View.GONE
            }
            1 -> {
                itemViewHolder?.definition1?.text = context.getString(R.string.definition_1_placeholder, currentReviewKanjiPair.second.mDefinitions[0])
                itemViewHolder?.definition2?.visibility = View.GONE
            }
            2 -> {
                itemViewHolder?.definition2?.visibility = View.VISIBLE
                itemViewHolder?.definition1?.text = context.getString(R.string.definition_1_placeholder, currentReviewKanjiPair.second.mDefinitions[0])
                itemViewHolder?.definition2?.text = context.getString(R.string.definition_2_placeholder, currentReviewKanjiPair.second.mDefinitions[1])
            }
        }

        // Check for the common tag
        if (currentReviewKanjiPair.second.is_common) {
            itemViewHolder?.commonTag?.visibility = View.VISIBLE
        } else {
            itemViewHolder?.commonTag?.visibility = View.GONE
        }

        // Check the jlpt level and set color and text
        when (currentReviewKanjiPair.second.jlptTag) {
            -1 -> itemViewHolder?.jlptTag?.visibility = View.GONE
            1 -> {
                itemViewHolder?.jlptTag?.visibility = View.VISIBLE
                itemViewHolder?.jlptTag?.text = context.getString(R.string.JLPTN1)
                itemViewHolder?.jlptTag?.backgroundTintList =
                        ColorStateList(arrayOf(IntArray(1)),
                                IntArray(1, { ContextCompat.getColor(context, R.color.colorJLPTN1)}))
            }
            2 -> {
                itemViewHolder?.jlptTag?.visibility = View.VISIBLE
                itemViewHolder?.jlptTag?.text = context.getString(R.string.JLPTN2)
                itemViewHolder?.jlptTag?.backgroundTintList =
                        ColorStateList(arrayOf(IntArray(1)),
                                IntArray(1, { ContextCompat.getColor(context, R.color.colorJLPTN2)}))
            }
            3 -> {
                itemViewHolder?.jlptTag?.visibility = View.VISIBLE
                itemViewHolder?.jlptTag?.text = context.getString(R.string.JLPTN3)
                itemViewHolder?.jlptTag?.backgroundTintList =
                        ColorStateList(arrayOf(IntArray(1)),
                                IntArray(1, { ContextCompat.getColor(context, R.color.colorJLPTN3)}))
            }
            4 -> {
                itemViewHolder?.jlptTag?.visibility = View.VISIBLE
                itemViewHolder?.jlptTag?.text = context.getString(R.string.JLPTN4)
                itemViewHolder?.jlptTag?.backgroundTintList =
                        ColorStateList(arrayOf(IntArray(1)),
                                IntArray(1, { ContextCompat.getColor(context, R.color.colorJLPTN4)}))
            }
            5 -> {
                itemViewHolder?.jlptTag?.visibility = View.VISIBLE
                itemViewHolder?.jlptTag?.text = context.getString(R.string.JLPTN5)
                itemViewHolder?.jlptTag?.backgroundTintList =
                        ColorStateList(arrayOf(IntArray(1)),
                                IntArray(1, { ContextCompat.getColor(context, R.color.colorJLPTN5)}))
            }
        }

        if (!currentReviewKanjiPair.second.url.isEmpty()) {
            // if there is jisho data show details link
            itemViewHolder?.detailsLink?.visibility = View.VISIBLE
            // Set an onclick to open the jisho url, and add an animation for the alpha
            itemViewHolder?.detailsLink?.alpha = 1.0f
            // Set a listener to know when the alpha ends to return to 1.0f alpha
            itemViewHolder?.detailsLink?.setOnClickListener {
                // set animation
                itemViewHolder.detailsLink.animate().alpha(0.2f)
                        .setDuration(500).setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        itemViewHolder.detailsLink.animate().alpha(1.0f)
                                .setDuration(500).start()
                    }

                }).start()
                // Set the url with an intent.
                val webpage = Uri.parse(currentReviewKanjiPair.second.url)
                val intent = Intent(Intent.ACTION_VIEW, webpage)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                }
            }
        }

        if (position == reviewKanjis.size) {
            itemViewHolder?.divider?.visibility = View.INVISIBLE
        }

        itemViewHolder?.delete?.setOnClickListener {
            removeItem(itemViewHolder.adapterPosition, currentReviewKanjiPair)

            Snackbar.make(fragmentRootView.review_recycler_parent,
                    context.getString(R.string.snackbar_word_placeholder,
                            currentReviewKanjiPair.second.word), Snackbar.LENGTH_LONG)
                    .setAction(context.getString(R.string.undo_snackbar), {
                        restoreItem(currentReviewKanjiPair, position)
                    }).show()
        }
    }

    /**
     * Helper fun to remove item from the recycler view.
     */
    fun removeItem(position: Int, currentKanji: Pair<Int, Kanji>) {
        kanjiDao.updateReviewKanji(context, currentKanji.first, false)
        // delete the item at the position swiped
        reviewKanjis.removeAt(position - 1)
        // notify the removal for animation purposes + 1 considering header
        notifyItemRemoved(position)
        if (reviewKanjis.size == 0) {
            notifyListener(true)
        }
    }

    /**
     * Helper fun to add item to the recycler view.
     */
    fun restoreItem(currentKanji: Pair<Int, Kanji>, position: Int) {
        kanjiDao.updateReviewKanji(context, currentKanji.first, true)
        reviewKanjis.add(position - 1, currentKanji)
        // notify item added by position
        notifyItemInserted(position)
        notifyListener(false)
    }

    override fun getItemViewType(position: Int): Int {
        // Return in a when loop.
        return when (position) {
        // If its the first item it must be the header.
            0 -> headerType
        // If its within range of 1 or the array size it must be an item.
            in 1..reviewKanjis.size -> itemType
        // If its above the list size it must be the footer.
            reviewKanjis.size + 1 -> footerType
            else -> itemType
        }
    }

    /**
     * In-class list item view holder implementation with on click function.
     */
    class ReviewWordItemViewHolder(itemView: View,
                                val reviewKanjis: MutableList<Pair<Int, Kanji>>,
                                val listener: (reviewKanji: Pair<Int, Kanji>) -> Unit)
        : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        init {
            // Set the view onClick listener to the function passed to the adapter's
            // constructor, i.e. defined in the fragment. Make sure the adapter position
            // is within the extracts and not a header or footer.
            itemView.setOnClickListener(this@ReviewWordItemViewHolder)
        }

        override fun onClick(v: View?) {
            // pass the clicked review kanji.
            if (adapterPosition > 0) {
                listener(reviewKanjis[adapterPosition - 1])
            }
        }

        val word: TextView = itemView.findViewById(R.id.review_title)
        val reading: TextView = itemView.findViewById(R.id.review_reading)
        val definition1: TextView = itemView.findViewById(R.id.review_definition_1)
        val definition2: TextView = itemView.findViewById(R.id.review_definition_2)
        val detailsLink: TextView = itemView.findViewById(R.id.review_details_link)
        val jlptTag: TextView = itemView.findViewById(R.id.review_jlpt_tag)
        val commonTag: TextView = itemView.findViewById(R.id.review_common_tag)
        val divider: View = itemView.findViewById(R.id.grey_line_divider)
        val delete: ImageView = itemView.findViewById(R.id.review_delete_button)
    }

    /**
     * In-class header view holder implementation.
     */
    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    /**
     * In-class footer view holder implementation.
     */
    class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}