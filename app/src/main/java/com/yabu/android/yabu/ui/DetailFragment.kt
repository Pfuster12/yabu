package com.yabu.android.yabu.ui

import android.app.Dialog
import android.os.Bundle
import android.os.Parcelable
import android.support.design.widget.BottomSheetDialog
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v4.app.Fragment
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

import com.yabu.android.yabu.R
import jsondataclasses.WikiExtract
import kotlinx.android.synthetic.main.fragment_detail.view.*
import org.parceler.Parcels
import utils.BundleKeys
import utils.WordScanner

/**
 * Fragment for the details of an extract screen.
 */
class DetailFragment : BottomSheetDialogFragment() {

    /**
     * Lazy init the parcelable unwrap into the current wikiExtract to have class wide access to it.
     */
    private val wikiExtract: WikiExtract? by lazy {
        val parcelable: Parcelable? = arguments?.getParcelable(BundleKeys.WIKI_EXTRACTS_BUNDLE)
        Parcels.unwrap<WikiExtract>(parcelable)
    }

    private lateinit var spannableString: SpannableString

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
     * Override onCreate to analyze vocabulary.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the clickable spans to the words in onCreate.
        setSpannable(wikiExtract?.extract)
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
        rootView.detail_title.text = wikiExtract?.title?.trim()

        // Set the link movement for the text view for clickable spans to work.
        rootView.detail_extract.movementMethod = LinkMovementMethod.getInstance()

        // Bind the clickable span string as text to the view.
        rootView.detail_extract.text = spannableString

        // Set thumbnail with Glide.
        GlideApp.with(context)
                .load(wikiExtract?.thumbnail?.source)
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.color.color500Grey)
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
        toolbarTitle.text = getString(R.string.bottom_sheet_toolbar_title_prefix, wikiExtract?.title)
    }

    /**
     * Private helper fun to set the clickable spans to the text
     * in order to open the definition pop-up.
     */
    private fun setSpannable(string: String?) {
        // Scan the extract text using the utils WordScanner class. Returns a list of
        // pair values of word and index range within the extract string.
        val pairs = WordScanner.getUtils().scanText(wikiExtract?.extract)

        // Create the spannable string
        spannableString = SpannableString(string)

        // Iterate through the pairs to assign a span to the string
        for (pair in pairs) {

            // Create a clickable span to set the onclick for a pop up
            val clickableSpan = object : ClickableSpan() {
                /**
                 * Override onClick to open the pop up.
                 */
                override fun onClick(widget: View?) {
                    Toast.makeText(this@DetailFragment.context,
                            "You clicked on me", Toast.LENGTH_SHORT).show()
                }
            }

            // Set the clickable span to the span string word, with inclusive indexes.
            spannableString
                    .setSpan(clickableSpan,
                            pair.first.last,
                            pair.first.first + 1,
                            SpannableString.SPAN_INCLUSIVE_INCLUSIVE)
        }
    }
}
