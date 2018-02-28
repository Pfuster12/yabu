package com.yabu.android.yabu.ui

import android.content.Context
import android.content.Intent
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.yabu.android.yabu.R
import jsondataclasses.WikiExtract

/**
 * Recycler View custom adapter with a header, footer and item view holder. Passes the adapter
 * data, the activity context and the listener callback to the activity.
 */
class ReadingRecyclerViewAdapter(private val wikiExtracts: MutableList<WikiExtract>,
                                 private val context: Context, private val listener: (extract: WikiExtract) -> Unit)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    /*
    Types of view holders to return for the function itemViewType
     */
    private val headerType = 100
    private val itemType = 101
    private val footerType = 102
    private val adType = 103

    /**
     * Override function that create the view holder object.
     */
    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
        // Start a return when condition with the type int.
        return when (viewType) {
            // If header type inflate the header resource.
            headerType -> HeaderViewHolder(inflateViewHolder(R.layout.reading_heading, parent))
            // If item type inflate the item resource.
            itemType -> ReadingItemViewHolder(inflateViewHolder(R.layout.reading_list_item, parent),
                    wikiExtracts, listener)
            adType -> AdItemViewHolder(inflateViewHolder(R.layout.reading_ad_item, parent))
            // If footer type inflate the footer resource.
            footerType -> FooterViewHolder(inflateViewHolder(R.layout.reading_footer, parent))
            // Else return the item layout as a default.
            else -> ReadingItemViewHolder(inflateViewHolder(R.layout.reading_list_item, parent),
                    wikiExtracts, listener)
        }
    }

    /**
     * Helper function to inflate a layout resource.
     */
    private fun inflateViewHolder(layoutId: Int, parent: ViewGroup?): View {
        return LayoutInflater.from(context).inflate(layoutId, parent, false)
    }

    /**
     * Override function that bind views in the view holder,
     * text to text views and images to image views.
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        // Start a when condition with the view type int.
        when (getItemViewType(position)) {
            // If header type then bind header vectors
            headerType -> {} // Do bind operations here
            // If item type then bind texts and images with wikiExtracts pojo.
            itemType -> {
                // Cast the holder to a reading item.
                val itemViewHolder: ReadingItemViewHolder? = holder as? ReadingItemViewHolder
                // Bind the views.
                bindListItemHolder(itemViewHolder, position)
            }
            adType -> {
                // Cast the holder as an ad item holder
                val adViewHolder: AdItemViewHolder? = holder as? AdItemViewHolder
                // Load ad and bind
                bindAdHolder(adViewHolder)
            }
            // If footer type bind footer texts and vectors.
            footerType -> {}// Do bind operations here
        }
    }

    /**
     * Helper function to bind extract properties to the view holder.
     */
    private fun bindListItemHolder(itemViewHolder: ReadingItemViewHolder?, position: Int) {
        // Position is minus 1 because of the header
        var currentPosition = position - 1
        if (position in 0..5) {
            currentPosition = position - 1
        } else if (position in 7..wikiExtracts.size + 1) {
            currentPosition = position - 2
        }
        // Extract current extract
        val currentExtract: WikiExtract = wikiExtracts[currentPosition]
        // Set title.
        itemViewHolder?.title?.text = currentExtract.title
        // Set text.
        itemViewHolder?.extract?.text = currentExtract.extract
        // Set the image views to gray scale
        val matrix = ColorMatrix()
        matrix.setSaturation(0f)
        val filter = ColorMatrixColorFilter(matrix)
        itemViewHolder?.thumbnail?.colorFilter = filter
        // Set thumbnail with Glide.
        GlideApp.with(context)
                .load(currentExtract.thumbnail?.source)
                .transition(withCrossFade())
                .placeholder(R.color.color500Grey)
                .error(R.drawable.ground_astronautmhdpi)
                .into(itemViewHolder?.thumbnail)
    }

    private fun bindAdHolder(adViewHolder: AdItemViewHolder?) {
        val adRequest = AdRequest.Builder().build()
        adViewHolder?.ad?.loadAd(adRequest)
    }

    /**
     * Override function to get the total items in the adapter.
     */
    override fun getItemCount(): Int {
        // Return the lists size with header and footer and ad
        return wikiExtracts.size + 1 + 1 + 1
    }

    override fun getItemViewType(position: Int): Int {
        // Return in a when loop.
        return when (position) {
            // If its the first item it must be the header.
            0 -> headerType
            // If its within range of 1 or the array size it must be an item.
            in 1..5 -> itemType
            6 -> adType
            in 7..wikiExtracts.size + 1 -> itemType
            // If its above the list size it must be the footer.
            wikiExtracts.size + 1 + 1 -> footerType
            else -> itemType
        }
    }

    /**
     * In-class list item view holder implementation with on click function.
     */
    class ReadingItemViewHolder(itemView: View,
                                val wikiExtracts: MutableList<WikiExtract>,
                                val listener: (extract: WikiExtract) -> Unit)
        : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        init {
            // Set the view onClick listener to the function passed to the adapter's
            // constructor, i.e. defined in the fragment. Make sure the adapter position
            // is within the extracts and not a header or footer.
            itemView.setOnClickListener(this@ReadingItemViewHolder)
        }

        override fun onClick(v: View?) {
            when (adapterPosition) {
                in 1..5 -> listener(wikiExtracts[adapterPosition - 1])
                in 7..wikiExtracts.size + 1 -> listener(wikiExtracts[adapterPosition - 2])
            }
        }

        val title: TextView = itemView.findViewById(R.id.list_item_title)
        val extract: TextView = itemView.findViewById(R.id.list_item_extract)
        val thumbnail: TopCropImageView = itemView.findViewById(R.id.list_item_thumbnail)
    }

    /**
     * In-class header view holder implementation.
     */
    class AdItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ad: AdView = itemView.findViewById(R.id.adView)
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