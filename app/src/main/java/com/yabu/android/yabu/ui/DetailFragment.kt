package com.yabu.android.yabu.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.support.design.widget.BottomSheetDialogFragment
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.yabu.android.yabu.R
import jsondataclasses.Kanji
import jsondataclasses.WikiExtract
import kotlinx.android.synthetic.main.callout_bubble.view.*
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

    /**
     * Override onCreate to analyze vocabulary.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // init the kanji list
        mKanjis = mutableListOf()

        // init the spannable
        if (wikiExtract?.extract != null) {
            spannableString = SpannableString(wikiExtract?.extract)
        } else {
            spannableString = SpannableString("")
            Toast.makeText(this@DetailFragment.context, "Oh no! There is no text.",
                    Toast.LENGTH_SHORT)
                    .show()
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

        // Prepare the view model when the activity is created. The async api call is sent here
        // and the live data object is set.
        if (!spannableString.isEmpty()) {
            prepareViewModel(rootView)
        }

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
     * Helper fun to prepare the view model to observe
     * the live data object of the kanji and int range pair
     */
    private fun prepareViewModel(rootView: View) {
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

                // Set the clickable spans with the Kanjis
                setSpannable(mKanjis, rootView)

                // update the spannable string to the text view
                rootView.detail_extract.text = spannableString
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

    /**
     * Private helper fun to set the clickable spans to the text
     * in order to open the definition pop-up.
     */
    private fun setSpannable(kanjis: MutableList<Pair<IntRange, Kanji>>, rootView: View) {
        // Iterate through the pairs to assign a span to the string
        for (kanji in kanjis) {
            // Set the boolean object to see when it was clicked
            val controlNumber = object {
                val showFurigana = 0
                val showExplanation = 1
                val hideEverything = 2
            }
            // Set the initial number
            var clickedNumber = controlNumber.showFurigana

            // Create the furigana text view
            val furiganaView = TextView(this@DetailFragment.context)
            furiganaView.tag = "tag" + kanji.first.first.toString()
            // Set the text to the reading from the kanji pojo
            furiganaView.text = kanji.second.reading
            // Set text view properties
            furiganaView.maxLines = 1
            furiganaView.setTextAppearance(this@DetailFragment.context, R.style.FuriganaStyle)

            //Set the furigana text view using the helper function.
            setFuriganaView(kanji, furiganaView)

            // Create a clickable span to set the onclick for a pop up
            val clickableSpan = object : ClickableSpan() {

                /**
                 * Override onClick to open the pop up.
                 */
                override fun onClick(widget: View?) {
                    // Start a when loop to see what state is the clicked kanji in
                    when (clickedNumber){
                        controlNumber.showFurigana -> {
                            // Kanji was clicked with furigana so we show explanation
                            hideCalloutBubble(rootView)
                            showFurigana(furiganaView)
                            // Set the state to show explanation
                            clickedNumber = controlNumber.showExplanation
                        }
                        controlNumber.showExplanation -> {
                            hideCalloutBubble(rootView)
                            // Send an async to get the word definition from jisho
                            setDefinitionViewModel(kanji, rootView, furiganaView)

                            // Set the state to hide everything
                            clickedNumber = controlNumber.hideEverything
                        }
                        controlNumber.hideEverything -> {
                            // Kanji was clicked with definition so we need to hide it
                            hideFurigana(furiganaView)
                            hideCalloutBubble(rootView)

                            // Set the state back to show furigana
                            clickedNumber = controlNumber.showFurigana
                        }
                    }
                }
            }

            // Set the clickable span to the span string word, with inclusive indexes.
            spannableString.setSpan(clickableSpan,
                            kanji.first.first,
                            kanji.first.last + 1,
                            SpannableString.SPAN_INCLUSIVE_INCLUSIVE)
        }
    }

    /**
     * Helper fun to add the furigana text view at the right location above the kanji.
     */
    private fun setFuriganaView(kanji: Pair<IntRange, Kanji>, furiganaView: TextView) {
        // Calculate the offsets
        val offsetPair = calculateOffsets(furiganaView, kanji)

        // Set the x and y coordinates to the furigana text view
        furiganaView.x = offsetPair.first
        // Raise the furigana to be above the text. The padding in the extract text view affects
        // the shift too.
        furiganaView.y = offsetPair.second

        // Add the furigana text view to the constraint layout parent.
        furigana_parent.addView(furiganaView)
        // Hide it until it is clicked on.
        hideFurigana(furiganaView)
    }

    /**
     * Helper fun to hide the furigana view
     */
    private fun hideFurigana(furigana: View) {
        furigana.visibility = View.GONE
    }

    /**
     * Helper fun to show the furigana view
     */
    private fun showFurigana(furigana: View) {
        furigana.visibility = View.VISIBLE
    }

    private fun setDefinitionViewModel(protoKanji: Pair<IntRange, Kanji>, rootView: View,
                                       furiganaView: TextView) {
        // Create the observer which updates the UI. Whenever the data changes, the new pojo list
        // is fed through and the UI can be updated.
        val observer: Observer<Kanji> = Observer { kanji ->
            // Check for null since addAll() accepts non-null
            if (kanji != null) {
                // Set the word title.
                rootView.callout_bubble.callout_title.text = kanji.word
                // Set the reading.
                rootView.callout_bubble.callout_reading.text = kanji.reading

                // Set the definitions.
                if (kanji.mDefinitions.size == 1) {
                    // There is only one definition so set the first and hide the second.
                    val def = "1. " + kanji.mDefinitions[0]
                    rootView.callout_bubble.callout_definition_alone.text = def
                    // Set visibility
                    showOneDefinition(rootView)
                } else if (kanji.mDefinitions.size >= 2) {
                    // There are more than 1 so add the two definitions.
                    val def = "1. " + kanji.mDefinitions[0]
                    val def2 = "2. " + kanji.mDefinitions[1]
                    rootView.callout_bubble.callout_definition_1.text = def
                    rootView.callout_bubble.callout_definition_2.text = def2
                    // Set visibility
                    showTwoDefinitions(rootView)
                } else {
                    // There is no definition available
                    val def = context.getString(R.string.no_definition)
                    rootView.callout_bubble.callout_definition_alone.text = def
                    // Set visibility
                    showOneDefinition(rootView)
                }

                // Kanji was clicked with furigana so we show explanation
                setCalloutBubble(rootView, protoKanji, furiganaView)
            }
        }

        // Make the api call to grab the furigana for each word in the extract spannable
        // Scan the extract text using the utils WordScanner class. Returns a list of
        // pair values of Kanjis and index range.
        mModel.getDefinitions(protoKanji.second)

        // Observe the LiveData in the view model which will be set to the kanji index pairs,
        // passing in the fragment as the LifecycleOwner and the observer created above.
        mModel.kanji.observe(this@DetailFragment, observer)
    }

    /**
     * Helper fun to set the text view visibility of callout bubble to one
     */
    private fun showOneDefinition(rootView: View) {
        rootView.callout_bubble.callout_definition_alone.visibility = View.VISIBLE
        rootView.callout_bubble.callout_definition_1.visibility = View.GONE
        rootView.callout_bubble.callout_definition_2.visibility = View.GONE
    }

    /**
     * Helper fun to set the text view visibility of callout bubble to two
     */
    private fun showTwoDefinitions(rootView: View) {
        rootView.callout_bubble.callout_definition_alone.visibility = View.GONE
        rootView.callout_bubble.callout_definition_1.visibility = View.VISIBLE
        rootView.callout_bubble.callout_definition_2.visibility = View.VISIBLE
    }

    /**
     * Helper fun to set the callout bubble in the right place
     */
    private fun setCalloutBubble(rootView: View, kanji: Pair<IntRange, Kanji>, furiganaView: TextView) {
        // Set the middle bubble svg initially
        rootView.callout_bubble.background = context.getDrawable(R.drawable.ic_callout_bubble_middle_shadow)

        val handler = Handler()
        handler.postDelayed(Runnable {
            // Calculate the bubble offset
            calculateCalloutBubbleOffset(rootView, furiganaView, kanji)
        }, 2000)
    }

    /**
     * Private fun to calculate the callout bubble offset in the screen
     */
    private fun calculateCalloutBubbleOffset(rootView: View, furiganaView: TextView,
                                             kanji: Pair<IntRange, Kanji>) {
        // Get the text view bounds height.
        val bounds = Rect()
        furiganaView.paint.getTextBounds(kanji.second.reading, 0, kanji.second.reading.length, bounds)
        val furiganaWidth = bounds.width()

        // Calculate the offset of furigana
        val offsetPair = calculateOffsets(furiganaView, kanji)
        // Offset the callout bubble subtracting his height
        rootView.callout_bubble.y = offsetPair.second - rootView.callout_bubble.height
        rootView.callout_bubble.x = offsetPair.first - (rootView.callout_bubble.width / 2) + (furiganaWidth / 2)

        // Grab the width and padding.
        val calloutWidth = rootView.callout_bubble.measuredWidth

        // Get metrics of screen device.
        val displayMetrics = DisplayMetrics()
        this@DetailFragment.activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels

        // Check if the callout bubble is out of the screen
        if (rootView.callout_bubble.x < 0) {
            // If it is out of the start side, change bubble to start svg and move to the edge with padding.
            rootView.callout_bubble.background =
                    context.getDrawable(R.drawable.ic_callout_bubble_start_shadow)
            // If it is from the start, set the x to the offset calculated.
            rootView.callout_bubble.x =  offsetPair.first
        } else if ((rootView.callout_bubble.x + calloutWidth) > screenWidth) {
            // If it is out of the end side, change bubble to end svg and move to the edge with padding.
            // If it is not make end x the last char of the start line plus the one charwidth
            // Get the layout of the text view
            val textViewLayout = detail_extract.layout
            // Span of one character
            val charWidth =
                    textViewLayout.getPrimaryHorizontal(1) - textViewLayout.getPrimaryHorizontal(0)

            rootView.callout_bubble.background =
                    context.getDrawable(R.drawable.ic_callout_bubble_end_shadow)
            // Set the callout to be at the end of the text + some char widths to center it.
            rootView.callout_bubble.x =
                    offsetPair.first - calloutWidth + (furiganaWidth / 2) + (charWidth / 1.2f)
        }
        showCalloutBubble(rootView)
    }

    /**
     * Helper fun to show the callout bubble
     */
    private fun showCalloutBubble(rootView: View) {
        rootView.callout_bubble.visibility = View.VISIBLE
    }

    /**
     * Helper fun to hide the callout bubble
     */
    private fun hideCalloutBubble(rootView: View) {
        rootView.callout_bubble.visibility = View.INVISIBLE
    }

    private fun calculateOffsets(furiganaView: TextView, kanji: Pair<IntRange, Kanji>): Pair<Float, Float> {
        // Get the text view bounds height
        val bounds = Rect()
        furiganaView.paint.getTextBounds(kanji.second.reading, 0, kanji.second.reading.length, bounds)
        val furiganaHeight = bounds.height()
        val furiganaWidth = bounds.width()

        // Get the layout of the text view
        val textViewLayout = detail_extract.layout
        // Get the line number of the current kanji
        val lineNumber = textViewLayout.getLineForOffset(kanji.first.first)
        // Get the top of the line (y coordinates) + the padding added to the text views to fit
        // top furigana
        val startYCoordinates = textViewLayout.getLineTop(lineNumber) + detail_extract.paddingTop + detail_title.measuredHeight + detail_title.layout.topPadding + detail_title.layout.bottomPadding

        /*
        Calculate the X coordinate to offset the furigana view. We also need to calculate whether
        the kanji spans two lines first, and change the calculation to be only in the first line.
         */
        // Line of the first kanji
        val startLine = textViewLayout.getLineForOffset(kanji.first.first)
        // Line of the first kanji + 1
        val startLine2 = textViewLayout.getLineForOffset(kanji.first.first + 1)
        // Line of the last kanji
        val endLine = textViewLayout.getLineForOffset(kanji.first.last + 1)

        // Span of one character
        var charWidth = textViewLayout.getPrimaryHorizontal(1) - textViewLayout.getPrimaryHorizontal(0)

        // Init the start and end x coordinates
        val startXCoordinates: Float
        val endXCoordinates: Float

        // Check if the start line is the same as the kanji + 1 start line
        if (startLine == startLine2) {
            // If it is the start x will be kanji + 1
            startXCoordinates = textViewLayout.getPrimaryHorizontal(kanji.first.first + 1)
        } else {
            // If not, the start x will be the first kanji
            startXCoordinates = textViewLayout.getPrimaryHorizontal(kanji.first.first)
        }

        // Check if the start line is the same as the end line
        if (startLine == endLine) {
            // If it is, make end x the last kanji + 1
            endXCoordinates = textViewLayout.getPrimaryHorizontal(kanji.first.last + 1)
        } else {
            // If it is not make end x the last char of the start line plus the one charwidth
            val textOffset = textViewLayout.getLineEnd(startLine) - 1
            if (kanji.first.first == kanji.first.last) {
                charWidth *= 2
            }

            endXCoordinates = textViewLayout.getPrimaryHorizontal(textOffset) + charWidth
        }

        // Get the mid coordinates of the kanji and add a
        // factor of mid of the text bound width to center it.
        val offsetX = startXCoordinates + ((endXCoordinates - startXCoordinates) / 2) - (furiganaWidth / 2.6)

        // Get the desired Y coordinate, a factor of the bounds height as a margin
        val offsetY = startYCoordinates - furiganaHeight - (furiganaHeight / 1.8)

        return Pair(offsetX.toFloat(), offsetY.toFloat())
    }
}
