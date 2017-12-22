package com.yabu.android.yabu.ui

import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

import com.yabu.android.yabu.R
import jsondataclasses.WikiExtract
import kotlinx.android.synthetic.main.fragment_detail.view.*
import org.parceler.Parcels

/**
 * Fragment for the details of an extract screen.
 */
class DetailFragment : Fragment() {

    /**
     * Lazy init the parcelable unwrap into the current wikiExtract to have class wide access to it.
     */
    private val wikiExtract: WikiExtract? by lazy {
        val parcelable: Parcelable? = arguments?.getParcelable(BundleKeys.WIKI_EXTRACTS_BUNDLE)
        Parcels.unwrap<WikiExtract>(parcelable)
    }

    companion object {
        /**
         * Helper function to create new instance passing the
         * wikiExtract as a bundle in the arguments.
         */
        fun newInstance(parcelable: Parcelable): DetailFragment {
            // Get an instance
            val fragment = DetailFragment()
            // Set the arguments to the extract passed in constructor
            val args = Bundle()
            args.putParcelable(BundleKeys.WIKI_EXTRACTS_BUNDLE, parcelable)
            fragment.arguments = args

            // return the loaded fragment
            return fragment
        }
    }

    /**
     * Override to inflate layout and bind views.
     */
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val rootView = inflater!!.inflate(R.layout.fragment_detail, container, false)

        // Set the toolbar title to Reading title.
        setToolbarTitle(rootView)

        // Bind the views with the current extract data
        rootView.detail_title.text = wikiExtract?.title
        rootView.detail_extract.text = wikiExtract?.extract?.trim()

        // Set thumbnail with Glide.
        GlideApp.with(context)
                .load(wikiExtract?.thumbnail?.source)
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.color.color500Grey)
                .error(R.drawable.ic_astronaut_flying)
                .centerCrop()
                .into(rootView.detail_thumbnail)

        // return the inflated view
        return rootView
    }

    /**
     * Helper function to set toolbar to the Review Words title.
     */
    private fun setToolbarTitle(rootView: View) {
        // Grab the title of the toolbar.
        val toolbarTitle: TextView = rootView
                .layout_toolbar.findViewById(R.id.toolbar_title)
        // Set the title of the toolbar to the Reading tab.
        toolbarTitle.text = getString(R.string.reading_page_title)
    }
}// Required empty public constructor
