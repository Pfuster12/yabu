package com.yabu.android.yabu.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.Parcelable
import android.support.design.widget.BottomSheetDialogFragment
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.yabu.android.yabu.R
import jsondataclasses.Kanji
import jsondataclasses.WikiExtract
import kotlinx.android.synthetic.main.fragment_detail.*
import kotlinx.android.synthetic.main.fragment_detail.view.*
import org.parceler.Parcels
import utils.BundleKeys
import viewmodel.JishoViewModel

/**
 * Fragment for the details of an extract screen.
 */
class DetailFragment : BottomSheetDialogFragment() {

    // The ViewModel to instantiate it through the provider.
    private lateinit var mModel: JishoViewModel

    // List of pairs for the Kanji pojo and the index range in the extract text
    private lateinit var mKanjis: MutableList<Pair<IntRange, Kanji>>

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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Prepare the view model when the activity is created. The async api call is sent here
        // and the live data object is set.
        prepareViewModel()
    }

    /**
     * Override onCreate to analyze vocabulary.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // init the kanji list
        mKanjis = mutableListOf()

        // Set the span string to the extract for an init without spans
        spannableString = SpannableString(wikiExtract?.extract)
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
    private fun setSpannable() {
        // Iterate through the pairs to assign a span to the string
        for (kanji in mKanjis) {

            // Create a clickable span to set the onclick for a pop up
            val clickableSpan = object : ClickableSpan() {

                /**
                 * Override onClick to open the pop up.
                 */
                override fun onClick(widget: View?) {
                    Toast.makeText(this@DetailFragment.context, kanji.second.reading, Toast.LENGTH_SHORT).show()
                    /*// Create the furigana text view
                    val furiganaView = TextView(this@DetailFragment.context)

                    // Set the text to the reading from the kanji pojo
                    furiganaView.text = kanji.second.reading
                    // Add the view to the parent
                    extract_text_parent.addView(furiganaView)*/
                }
            }

            // Set the clickable span to the span string word, with inclusive indexes.
            spannableString
                    .setSpan(clickableSpan,
                            kanji.first.last,
                            kanji.first.first + 1,
                            SpannableString.SPAN_INCLUSIVE_INCLUSIVE)
        }
    }

    private fun prepareViewModel() {
        // Get the ViewModel for the Jisho Detail Fragment
        mModel = ViewModelProviders.of(this@DetailFragment)
                .get(JishoViewModel::class.java)

        // Create the observer which updates the UI. Whenever the data changes, the new pojo list
        // is fed through and the UI can be updated.
        val observer: Observer<MutableList<Pair<IntRange, Kanji>>> = Observer { pairs ->
            // Check for null since addAll() accepts non-null
            if (pairs != null) {
                // Clear the list of the old pairs
                mKanjis.clear()

                // Add the received pairs list to the global list
                mKanjis.addAll(pairs)
                // Set the clickable spans to the words in onCreate.
                setSpannable()
            }
        }

        // Make the api call to grab the furigana for each word in the extract spannable
        // Scan the extract text using the utils WordScanner class. Returns a list of
        // pair values of Kanjis and index range.
        mModel.loadKanjis(wikiExtract?.extract)

        // Observe the LiveData in the view model which will be set to the kanji index pairs,
        // passing in the fragment as the LifecycleOwner and the observer created above.
        mModel.kanjis.observe(this@DetailFragment, observer)
    }
}
