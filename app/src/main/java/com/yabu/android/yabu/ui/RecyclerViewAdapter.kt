package com.yabu.android.yabu.ui

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.yabu.android.yabu.R
import pojos.WikiExtract

/**
 * Recycler View custom adapter with a header, footer and item view holder.
 */
class RecyclerViewAdapter(private val wikiExtracts: MutableList<WikiExtract>, private val context: Context)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    /*
    Types of view holders to return for the function itemViewType
     */
    private val headerType = 100
    private val itemType = 101
    private val footerType = 102

    /**
     * Override function that create the view holder object.
     */
    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
        // Start a return when condition with the type int.
        return when (viewType) {
            // If header type inflate the header resource.
            headerType -> HeaderViewHolder(inflateViewHolder(R.layout.reading_heading, parent))
            // If item type inflate the item resource.
            itemType -> ReadingItemViewHolder(inflateViewHolder(R.layout.reading_list_item, parent))
            // If footer type inflate the footer resource.
            footerType -> FooterViewHolder(inflateViewHolder(R.layout.reading_footer, parent))
            // Else return the item layout as a default.
            else -> ReadingItemViewHolder(inflateViewHolder(R.layout.reading_list_item, parent))
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
            // If item type then bind texts and images with wikiExtracts pojo
            itemType -> {
                val itemViewHolder: ReadingItemViewHolder? = holder as? ReadingItemViewHolder
                val currentPosition = position
                val currentExtract: WikiExtract = wikiExtracts[0]
                itemViewHolder?.title?.text = currentExtract.titleExtract
                itemViewHolder?.extract?.text = currentExtract.textExtract
            }
            // If footer type bind footer texts and vectors.
            footerType -> {}// Do bind operations here
        }
    }

    /**
     * Override function to get the total items in the adapter.
     */
    override fun getItemCount(): Int {
        // Return the lists side.
        return wikiExtracts.size + 1 + 1
    }

    override fun getItemViewType(position: Int): Int {
        // Return in a when loop.
        return when (position) {
            // If its the first item it must be the header.
            0 -> itemType
            // If its within range of 1 or the array size it must be an item.
            in 1..wikiExtracts.size -> itemType
            // If its above the list size it must be the footer.
            wikiExtracts.size -> footerType
            else -> itemType
        }
    }

    /**
     * In-class list item view holder implementation with on click function.
     */
    class ReadingItemViewHolder(itemView: View)
        : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        val title: TextView = itemView.findViewById(R.id.list_item_title)
        val extract: TextView = itemView.findViewById(R.id.list_item_extract)
        val thumbnail: ImageView = itemView.findViewById(R.id.list_item_thumbnail)

        /**
         * onClick function for the view holder of the recycler view.
         */
        override fun onClick(v: View?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
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