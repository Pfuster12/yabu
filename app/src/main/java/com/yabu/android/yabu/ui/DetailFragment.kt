package com.yabu.android.yabu.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.ActionBar
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.preference.PreferenceManager
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v4.content.ContextCompat
import android.support.v4.widget.NestedScrollView
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.transition.TransitionManager
import android.util.ArrayMap
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.firebase.analytics.FirebaseAnalytics
import com.yabu.android.yabu.R
import jsondataclasses.Kanji
import jsondataclasses.WikiExtract
import kotlinx.android.synthetic.main.callout_bubble.view.*
import kotlinx.android.synthetic.main.callout_bubble_loading.view.*
import kotlinx.android.synthetic.main.fragment_detail.*
import kotlinx.android.synthetic.main.fragment_detail.view.*
import org.parceler.Parcels
import repository.JishoRepository
import sql.KanjisSQLDao
import utils.BundleKeys
import utils.MiscUtils
import viewmodel.JishoViewModel
import viewmodel.WikiExtractsViewModel
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

/**
 * Fragment for the details of an extract screen.
 */
class DetailFragment : BottomSheetDialogFragment() {

    // The ViewModel to instantiate it through the provider.
    private lateinit var mModel: JishoViewModel

    // The ViewModel to instantiate it through the provider.
    private lateinit var mModelWiki: WikiExtractsViewModel

    // List of pairs for the Kanji pojo and the index range in the extract text
    private lateinit var mKanjis: MutableList<Pair<IntRange, Kanji>>

    private lateinit var mPrefs: SharedPreferences

    private var misRead: Boolean = false

    /**
     * Lazy init the parcelable unwrap into the current wikiExtract to have class wide access to it.
     */
    private val wikiExtract: WikiExtract? by lazy {
        val parcelable: Parcelable? = arguments?.getParcelable(BundleKeys.WIKI_EXTRACTS_BUNDLE)
        Parcels.unwrap<WikiExtract>(parcelable)
    }

    private lateinit var spannableString: SpannableString

    private val kanjiDao = KanjisSQLDao.getInstance()

    companion object {
        val READ_BOOL_KEY = "com.yabu.android.yabu.READ_BOOL_KEY"
        val KANJIS_KEY = "com.yabu.android.yabu.KANJIS_KEY"

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

        // Get the ViewModel for the Jisho Detail Fragment
        mModelWiki = ViewModelProviders.of(this@DetailFragment)
                .get(WikiExtractsViewModel::class.java)

        if (savedInstanceState == null) {
            // init the kanji list
            mKanjis = mutableListOf()

            val readBool =  mModelWiki.isRead(context, wikiExtract)

            if (readBool != null) {
                misRead = readBool
            } else {
                misRead = false
            }
        } else {
            val maps: HashMap<List<Int>, Kanji> =
                    Parcels.unwrap(savedInstanceState.getParcelable(DetailFragment.KANJIS_KEY))
            val pairKanjisNew = mutableListOf<Pair<IntRange, Kanji>>()
            for (map in maps) {
                val first = map.key[0]
                val last = map.key[1]

                pairKanjisNew.add(Pair(IntRange(first, last), map.value))
            }
            mKanjis = pairKanjisNew

            misRead = savedInstanceState.getBoolean(DetailFragment.READ_BOOL_KEY)
        }

        // init the spannable
        if (wikiExtract?.extract != null) {
            spannableString = SpannableString(wikiExtract?.extract)
        } else {
            spannableString = SpannableString("")
            Toast.makeText(this@DetailFragment.context, context.getString(R.string.no_extract_text),
                    Toast.LENGTH_SHORT)
                    .show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        // save isRead boolean
        outState?.putBoolean(DetailFragment.READ_BOOL_KEY, misRead)

        // Change pair list to map for parceler purposes
        val map = hashMapOf<MutableList<Int>, Kanji>()
        for (kanji in mKanjis) {
            map.put(mutableListOf(kanji.first.first, kanji.first.last), kanji.second)
        }
        outState?.putParcelable(DetailFragment.KANJIS_KEY, Parcels.wrap(map))

        super.onSaveInstanceState(outState)
    }

    /**
     * Override to inflate layout and bind views.
     */
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val rootView = inflater!!.inflate(R.layout.fragment_detail, container, false)

        if (savedInstanceState != null) {
            // Set the clickable spans with the Kanjis
            setSpannable(mKanjis, rootView)
            // update the spannable string to the text view
            rootView.detail_extract.text = spannableString
        }

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

        // Set an on scroll listener to hide the callout bubble when scrolling.
        rootView.scroll_parent.setOnScrollChangeListener(object : NestedScrollView.OnScrollChangeListener {
            override fun onScrollChange(v: NestedScrollView?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) {
                hideCalloutBubble(rootView)
            }
        })

        if (misRead) {
            rootView.read_button.text = context?.getString(R.string.read_that_button_false)
        }

        // set an onclick to the read button to set if its read
        rootView.read_button.setOnClickListener {
            if (misRead) {
                misRead = false
                rootView.read_button.animate().alpha(0.2f).setListener(object : AnimatorListenerAdapter() {
                    /**
                     * Override animation end to set visibility.
                     */
                    override fun onAnimationEnd(animation: Animator?) {
                        rootView.read_button.animate().alpha(1.0f).start()
                        rootView.read_button.text = context?.getString(R.string.read_that_button)
                    }
                }).start()
                setArticlesRead(misRead)
            } else {
                misRead = true
                rootView.read_button.animate().alpha(0.2f).setListener(object : AnimatorListenerAdapter() {
                    /**
                     * Override animation end to set visibility.
                     */
                    override fun onAnimationEnd(animation: Animator?) {
                        rootView.read_button.animate().alpha(1.0f).start()
                        rootView.read_button.text = context?.getString(R.string.read_that_button_false)
                    }
                }).start()
                setArticlesRead(misRead)
            }
        }

        // return the inflated view
        return rootView
    }

    /**
     * Helper fun to set the articles to persist for data graph in user fragment
     */
    private fun setArticlesRead(isRead: Boolean) {
        mModelWiki.setRead(context, wikiExtract, isRead)
        // grab the preferences
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context)

        // get todays day
        val today = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)

        // get the number of articles read
        val articlesRead = mPrefs.getInt(today.toString(), 0)

        val editor = mPrefs.edit()

        if (isRead) {
            // put the new number
            editor.putInt(today.toString(), articlesRead + 1)
            editor.apply()
        } else {
            if (articlesRead > 0) {
                editor.putInt(today.toString(), articlesRead - 1)
                editor.apply()
            }
        }
    }


    /**
     * Helper function to set toolbar to the Review Words title.
     */
    private fun setToolbarTitle(rootView: View) {
        // Grab the title of the toolbar.
        val toolbarTitle: TextView? = rootView
                .layout_toolbar?.findViewById(R.id.toolbar_title)
        // Set the title of the toolbar to the Reading tab.
        toolbarTitle?.text = getString(R.string.bottom_sheet_toolbar_title_prefix, wikiExtract?.title)
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
        mModel.loadKanjis(context, wikiExtract)

        // Observe the LiveData in the view model which will be set to the kanji index pairs,
        // passing in the fragment as the LifecycleOwner and the observer created above.
        mModel.kanjis.observe(this@DetailFragment, observer)
    }

    /**
     * Private helper fun to set the clickable spans to the text
     * in order to open the definition pop-up.
     */
    private fun setSpannable(kanjis: MutableList<Pair<IntRange, Kanji>>, rootView: View) {
        TransitionManager.beginDelayedTransition(rootView.furigana_parent)
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
            setFuriganaView(rootView, kanji, furiganaView)

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
                            calculateLoadingCallout(rootView, furiganaView, kanji)
                            showLoadingCallout(rootView, furiganaView, kanji)
                            // Send an async to get the word definition from jisho
                            setDefinitionViewModel(kanji, rootView, furiganaView)

                            // Set the state to hide everything
                            clickedNumber = controlNumber.hideEverything
                        }
                        controlNumber.hideEverything -> {
                            // Kanji was clicked with definition so we need to hide it
                            hideFurigana(furiganaView)
                            hideCalloutBubble(rootView)
                            hideLoadingCallout(rootView)

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
     * Helper fun to set the view model to the jisho view model and the jisho repo
     * linked to that.
     */
    private fun setDefinitionViewModel(protoKanji: Pair<IntRange, Kanji>, rootView: View,
                                       furiganaView: TextView) {
        // Create the observer which updates the UI. Whenever the data changes, the new pojo list
        // is fed through and the UI of the callout bubble can be updated.
        val observer: Observer<Pair<Int?, Kanji?>> = Observer { kanji ->
            // Check for null since addAll() accepts non-null
            if (kanji != null) {
                setCalloutBubble(rootView, Pair(protoKanji.first, kanji.second), furiganaView, kanji.first)
            }
        }

        // Make the api call to grab the furigana for each word in the extract spannable
        // Scan the extract text using the utils WordScanner class. Returns a list of
        // pair values of Kanjis and index range.
        mModel.getDefinitions(context, protoKanji.second)

        // Observe the LiveData in the view model which will be set to the kanji index pairs,
        // passing in the fragment as the LifecycleOwner and the observer created above.
        mModel.kanji.observe(this@DetailFragment, observer)
    }

    /**
     * Helper fun to add the furigana text view at the right location above the kanji.
     */
    private fun setFuriganaView(rootView: View, kanji: Pair<IntRange, Kanji>, furiganaView: TextView) {
        // Calculate the offsets
        val handler = Handler()
        handler.postDelayed(Runnable {
            val offsetPair = calculateFuriganaOffsets(rootView, furiganaView, kanji)

            // Set the x and y coordinates to the furigana text view
            furiganaView.x = offsetPair.first
            // Raise the furigana to be above the text. The padding in the extract text view affects
            // the shift too.
            furiganaView.y = offsetPair.second

            // Add the furigana text view to the constraint layout parent.
            furigana_parent.addView(furiganaView)
            // Hide it until it is clicked on.
            hideFuriganaNoDelay(furiganaView)
        }, 1000)
    }

    /**
     * Helper fun to hide the furigana view
     */
    private fun hideFurigana(furigana: View) {
        furigana.animate().setListener(object : AnimatorListenerAdapter() {
            /**
             * Override animation end to set visibility.
             */
            override fun onAnimationEnd(animation: Animator?) {
                furigana.visibility = View.GONE
            }
        }).alpha(0f).setStartDelay(500).start()
    }

    /**
     * Helper fun to hide the furigana view
     */
    private fun hideFuriganaNoDelay(furigana: View) {
        furigana.alpha = 0f
        furigana.visibility = View.GONE
    }

    /**
     * Helper fun to show the furigana view
     */
    private fun showFurigana(furigana: View) {
        furigana.visibility = View.VISIBLE
        furigana.animate().alpha(1f).setListener(null).start()
    }

    private fun setCalloutBubble(rootView: View, kanji: Pair<IntRange, Kanji?>,
                                 furiganaView: TextView, kanjiId: Int?) {
        // Set the word title.
        rootView.callout_bubble.callout_title.text = kanji.second?.word
        // Set the reading.
        rootView.callout_bubble.callout_reading.text = kanji.second?.reading
        // Set the tags
        setCalloutTags(kanji.second, rootView)
        // set the review button according to review boolean
        val kanjiWord = kanji.second
        if (kanjiWord != null) {
            rootView.callout_bubble.callout_review_button.isSelected = kanjiWord.isReview

            if (kanjiWord.isReview) {
                rootView.callout_bubble.callout_review_button.setColorFilter(
                        ContextCompat.getColor(context, R.color.colorAccent), PorterDuff.Mode.SRC_ATOP)
            } else {
                rootView.callout_bubble.callout_review_button.setColorFilter(
                        ContextCompat.getColor(context, R.color.color500Grey), PorterDuff.Mode.SRC_ATOP)
            }

            if (!kanjiWord.url.isEmpty()) {
                // if there is jisho data show details link
                rootView.callout_bubble.callout_details_link.visibility = View.VISIBLE
                // Set an onclick to open the jisho url, and add an animation for the alpha
                rootView.callout_bubble.callout_details_link.alpha = 1.0f
                // Set a listener to know when the alpha ends to return to 1.0f alpha
                rootView.callout_bubble.callout_details_link.setOnClickListener {
                    // set animation
                    rootView.callout_bubble.callout_details_link.animate().alpha(0.2f)
                            .setDuration(500).setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator?) {
                            rootView.callout_bubble.callout_details_link.animate().alpha(1.0f)
                                    .setDuration(500).start()
                        }

                    }).start()
                    // Set the url with an intent.
                    val webpage = Uri.parse(kanji.second?.url)
                    val intent = Intent(Intent.ACTION_VIEW, webpage)
                    if (intent.resolveActivity(context.packageManager) != null) {
                        startActivity(intent)
                    }
                }
            } else {
                // if there isnt internet data, hide the link.
                rootView.callout_bubble.callout_details_link.visibility = View.INVISIBLE
            }

            // Set the definitions.
            when (kanjiWord.mDefinitions?.size) {
                1 -> {
                    // There is only one definition so set the first and hide the second.
                    rootView.callout_bubble.callout_definition_alone.text = context.getString(R.string.definition_1_placeholder, kanji.second!!.mDefinitions[0])
                    // Set visibility
                    showOneDefinition(rootView)
                }
                2 -> {
                    // There are more than 1 so add the two definitions.
                    rootView.callout_bubble.callout_definition_alone.text = context.getString(R.string.definition_1_placeholder, kanji.second!!.mDefinitions[0])
                    rootView.callout_bubble.callout_definition_1.text = context.getString(R.string.definition_1_placeholder, kanji.second!!.mDefinitions[0])
                    rootView.callout_bubble.callout_definition_2.text = context.getString(R.string.definition_2_placeholder, kanji.second!!.mDefinitions[1])
                    // Set visibility
                    showTwoDefinitions(rootView)
                }
                else -> {
                    // There is no definition available
                    val def = context.getString(R.string.no_definition)
                    // If there is no kanji loaded at all,
                    if (kanjiWord.url.isEmpty()) {
                        rootView.callout_bubble.callout_definition_alone.text = def
                    }
                    // Set visibility
                    showOneDefinition(rootView)
                }
            }
        }

        rootView.callout_bubble.callout_review_button.setOnClickListener {
            if (!rootView.callout_bubble.callout_review_button.isSelected) {
                // update the database on worker thread
                kanjiDao.updateReviewKanji(context, kanjiId, true)
                // set selected state
                rootView.callout_bubble.callout_review_button.isSelected = true
                rootView.callout_bubble.callout_review_button.setColorFilter(
                        ContextCompat.getColor(context, R.color.colorAccent), PorterDuff.Mode.SRC_ATOP)
            } else {
                // update the database on worker thread
                kanjiDao.updateReviewKanji(context, kanjiId, false)
                // set the drawable to be unselected state
                rootView.callout_bubble.callout_review_button.isSelected = false
                rootView.callout_bubble.callout_review_button.setColorFilter(
                        ContextCompat.getColor(context, R.color.color500Grey), PorterDuff.Mode.SRC_ATOP)
            }
        }

        // Kanji was clicked with furigana so we show explanation
        setCalloutBubblePosition(rootView, kanji, furiganaView)
    }

    private fun setCalloutTags(kanji: Kanji?, rootView: View) {
        // Check for the common tag
        if (kanji != null) {
            if (kanji.is_common) {
                rootView.callout_bubble.callout_common_tag.visibility = View.VISIBLE
            } else {
                rootView.callout_bubble.callout_common_tag.visibility = View.GONE
            }
        }

        // Check the jlpt level and set color and text
        when (kanji?.jlptTag) {
            -1 -> rootView.callout_bubble.callout_jlpt_tag.visibility = View.GONE
            1 -> {
                rootView.callout_bubble.callout_jlpt_tag.visibility = View.VISIBLE
                rootView.callout_bubble.callout_jlpt_tag.text = getString(R.string.JLPTN1)
                rootView.callout_bubble.callout_jlpt_tag.backgroundTintList =
                        ColorStateList(arrayOf(IntArray(1)),
                                IntArray(1, {ContextCompat.getColor(context, R.color.colorJLPTN1)}))
            }
            2 -> {
                rootView.callout_bubble.callout_jlpt_tag.visibility = View.VISIBLE
                rootView.callout_bubble.callout_jlpt_tag.text = getString(R.string.JLPTN2)
                rootView.callout_bubble.callout_jlpt_tag.backgroundTintList =
                        ColorStateList(arrayOf(IntArray(1)),
                                IntArray(1, {ContextCompat.getColor(context, R.color.colorJLPTN2)}))
            }
            3 -> {
                rootView.callout_bubble.callout_jlpt_tag.visibility = View.VISIBLE
                rootView.callout_bubble.callout_jlpt_tag.text = getString(R.string.JLPTN3)
                rootView.callout_bubble.callout_jlpt_tag.backgroundTintList =
                        ColorStateList(arrayOf(IntArray(1)),
                                IntArray(1, {ContextCompat.getColor(context, R.color.colorJLPTN3)}))
            }
            4 -> {
                rootView.callout_bubble.callout_jlpt_tag.visibility = View.VISIBLE
                rootView.callout_bubble.callout_jlpt_tag.text = getString(R.string.JLPTN4)
                rootView.callout_bubble.callout_jlpt_tag.backgroundTintList =
                        ColorStateList(arrayOf(IntArray(1)),
                                IntArray(1, {ContextCompat.getColor(context, R.color.colorJLPTN4)}))
            }
            5 -> {
                rootView.callout_bubble.callout_jlpt_tag.visibility = View.VISIBLE
                rootView.callout_bubble.callout_jlpt_tag.text = getString(R.string.JLPTN5)
                rootView.callout_bubble.callout_jlpt_tag.backgroundTintList =
                        ColorStateList(arrayOf(IntArray(1)),
                                IntArray(1, {ContextCompat.getColor(context, R.color.colorJLPTN5)}))
            }
        }
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
    private fun setCalloutBubblePosition(rootView: View, kanji: Pair<IntRange, Kanji?>, furiganaView: TextView) {
        // Set the padding bottom
        rootView.detail_padding_bottom.visibility = View.GONE

        val handler = Handler()
        handler.postDelayed(Runnable {
            // Calculate the bubble offset
            calculateCalloutBubbleOffset(rootView, furiganaView, kanji)
        }, 1000)
    }

    /**
     * Helper fun to show the loading callout
     */
    private fun showLoadingCallout(rootView: View, furiganaView: TextView, kanji: Pair<IntRange, Kanji>) {
        calculateLoadingCallout(rootView, furiganaView, kanji)
        rootView.callout_bubble_loading.visibility = View.VISIBLE
    }

    /**
     * Helper fun to hide the loading callout
     */
    private fun hideLoadingCallout(rootView: View) {
        rootView.callout_bubble_loading.visibility = View.INVISIBLE
    }

    /**
     * Helper fun to calculate the loading callout bubble.
     */
    private fun calculateLoadingCallout(rootView: View, furiganaView: TextView, kanji: Pair<IntRange, Kanji>) {
        // Get the text view bounds height.
        val bounds = Rect()
        furiganaView.paint.getTextBounds(kanji.second.reading, 0, kanji.second.reading.length, bounds)
        val furiganaWidth = bounds.width()

        // Calculate the offset of furigana
        val offsetPair = calculateFuriganaOffsets(rootView, furiganaView, kanji)

        // Offset the callout bubble subtracting his height
        rootView.callout_bubble_loading.y = offsetPair.second - rootView.callout_bubble_loading.height
        rootView.callout_bubble_loading.x = offsetPair.first - (rootView.callout_bubble_loading.width / 2) + (furiganaWidth / 2)

    }

    /**
     * Private fun to calculate the callout bubble offset in the screen
     */
    private fun calculateCalloutBubbleOffset(rootView: View, furiganaView: TextView,
                                             kanji: Pair<IntRange, Kanji?>) {
        // Set the middle bubble svg initially
        rootView.callout_bubble.background = context.getDrawable(R.drawable.ic_callout_bubble_middle_shadow)
        setPaddingForBottomCallout(rootView,36f)
        // Set the bottom bubble margin to be gone
        rootView.callout_bubble.callout_title_margin.visibility = View.GONE
        setMaxWidth(MiscUtils.getUtils().pxFromDp(context, 220f).toInt(), rootView)

        val kanjiWord = kanji.second
        if (kanjiWord != null) {
            // Get the text view bounds height.
            val bounds = Rect()
            furiganaView.paint.getTextBounds(kanji.second?.reading, 0, kanjiWord.reading.length, bounds)
            val furiganaWidth = bounds.width()

            // Get the layout of the text view
            val textViewLayout = detail_extract.layout

            // Calculate the offset of furigana
            val offsetPair = calculateFuriganaOffsets(rootView, furiganaView, kanji)
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
                // Check first if we need the bottom variant
                if (rootView.callout_bubble.y < 0) {
                    rootView.callout_bubble.background =
                            context.getDrawable(R.drawable.ic_callout_bubble_start_bottom_shadow)

                    // Lower the callout to be under the character.
                    val lineNumber = textViewLayout.getLineForOffset(kanji.first.first)
                    val lineBottom = textViewLayout.getLineBottom(lineNumber + 1)

                    rootView.callout_bubble.y = lineBottom.toFloat()
                    rootView.callout_bubble.callout_title_margin.visibility = View.INVISIBLE

                    // Set the padding to be less
                    setPaddingForBottomCallout(rootView, 20f)
                } else {
                    rootView.callout_bubble.background =
                            context.getDrawable(R.drawable.ic_callout_bubble_start_shadow)
                    rootView.callout_bubble.callout_title_margin.visibility = View.GONE
                }
                // If it is from the start, set the x to the offset calculated.
                rootView.callout_bubble.x =  offsetPair.first
            } else if ((rootView.callout_bubble.x + calloutWidth) > screenWidth) {
                // If it is out of the end side, change bubble to end svg and move to the edge with padding.
                // If it is not make end x the last char of the start line plus the one charwidth
                // Span of one character
                val charWidth =
                        textViewLayout.getPrimaryHorizontal(1) - textViewLayout.getPrimaryHorizontal(0)

                if (rootView.callout_bubble.y < 0) {
                    rootView.callout_bubble.background =
                            context.getDrawable(R.drawable.ic_callout_bubble_end_bottom_shadow)

                    // Lower the callout to be under the character.
                    val lineNumber = textViewLayout.getLineForOffset(kanji.first.first)
                    val lineBottom = textViewLayout.getLineBottom(lineNumber + 1)

                    rootView.callout_bubble.y = lineBottom.toFloat()
                    rootView.callout_bubble.callout_title_margin.visibility = View.INVISIBLE

                    // Set the padding to be less
                    setPaddingForBottomCallout(rootView, 20f)
                } else {
                    rootView.callout_bubble.background =
                            context.getDrawable(R.drawable.ic_callout_bubble_end_shadow)
                    rootView.callout_bubble.callout_title_margin.visibility = View.GONE
                }
                // Set the callout to be at the end of the text + some char widths to center it.
                rootView.callout_bubble.x =
                        offsetPair.first - calloutWidth + (furiganaWidth / 2) + (charWidth / 1.2f)
            }

            // Check if the bubble goes off the layout on top and is the middle callout
            if (rootView.callout_bubble.y < 0) {
                // If it does set the bottom variant of the callout.
                rootView.callout_bubble.background =
                        context.getDrawable(R.drawable.ic_callout_bubble_middle_bottom_shadow)

                // Lower the callout to be under the character.
                val lineNumber = textViewLayout.getLineForOffset(kanji.first.first)
                val lineBottom = textViewLayout.getLineBottom(lineNumber + 1)

                rootView.callout_bubble.y = lineBottom.toFloat()
                rootView.callout_bubble.callout_title_margin.visibility = View.INVISIBLE

                // Set the padding to be less
                setPaddingForBottomCallout(rootView, 20f)
            }

            // Check if callout is overflows at the bottom.
            if (rootView.callout_bubble.y + rootView.callout_bubble.height > rootView.furigana_parent.height) {
                val paddingHeight = (rootView.callout_bubble.y + rootView.callout_bubble.height) -
                        rootView.furigana_parent.height +
                        MiscUtils.getUtils().pxFromDp(context, 16f)

                val params = rootView.detail_padding_bottom.layoutParams
                params.height = paddingHeight.toInt()
                params.width = ActionBar.LayoutParams.MATCH_PARENT
                rootView.detail_padding_bottom.layoutParams = params
                rootView.detail_padding_bottom.visibility = View.VISIBLE
            }

            /*
            Calculate whether the new callout bubble is out of the screen width because of the text
            and reduce the max width of the text to change this.
             */
            val padding = rootView.detail_extract.paddingStart
            if (rootView.callout_bubble.x < 0) {
                // The bubble is out of the start so calculate what the max width should be
                // to be in plus padding 16dp
                val endXCoordinate = rootView.callout_bubble.x + calloutWidth

                val maxWidth = (endXCoordinate - padding) - (padding * 2)
                // Set the max width of the text views
                setMaxWidth(maxWidth.toInt(), rootView)
            } else if ((rootView.callout_bubble.x + calloutWidth) > screenWidth) {
                // The bubble is out of the end so calculate what the max width should be
                // to be in minus padding 16dp
                val endXCoordinate = screenWidth - padding

                val maxWidth = (endXCoordinate - rootView.callout_bubble.x) - (padding * 2)
                // Set the max width of the text views
                setMaxWidth(maxWidth.toInt(), rootView)
            }
            // Hide the loading spinner
            hideLoadingCallout(rootView)
            // Show the callout
            showCalloutBubble(rootView)

            if (furiganaView.visibility != View.VISIBLE) {
                hideCalloutBubble(rootView)
            }
        }
    }

    /**
     * Helper fun to set the max width of the text views to fit the screen.
     */
    private fun setMaxWidth(maxWidth: Int, rootView: View) {
        // Set the max width of the text views
        rootView.callout_bubble.callout_definition_1.maxWidth = maxWidth
        rootView.callout_bubble.callout_definition_2.maxWidth = maxWidth
        rootView.callout_bubble.callout_definition_alone.maxWidth = maxWidth
    }

    /**
     * Helper fun to set the padding bottom of the callout if it changes to a bottom bubble.
     */
    private fun setPaddingForBottomCallout(rootView: View, bottomPadding: Float) {
        rootView.callout_bubble.callout_details_link.setPaddingRelative(
                MiscUtils.getUtils().pxFromDp(context, 16f).toInt(),
                MiscUtils.getUtils().pxFromDp(context, 4f).toInt(),
                MiscUtils.getUtils().pxFromDp(context, 16f).toInt(),
                MiscUtils.getUtils().pxFromDp(context, bottomPadding).toInt())
    }

    /**
     * Helper fun to show the callout bubble
     */
    private fun showCalloutBubble(rootView: View) {
        rootView.callout_bubble.animate().alpha(1f).setListener(null).start()
        rootView.callout_bubble.visibility = View.VISIBLE
    }

    /**
     * Helper fun to hide the callout bubble
     */
    private fun hideCalloutBubble(rootView: View) {
        rootView.callout_bubble.animate().setListener(object : AnimatorListenerAdapter() {
            /**
             * Override animation end to set visibility.
             */
            override fun onAnimationEnd(animation: Animator?) {
                rootView.callout_bubble.visibility = View.INVISIBLE
                // Set the padding bottom
                rootView.detail_padding_bottom.visibility = View.GONE
            }
        }).alpha(0f).start()
    }

    private fun calculateFuriganaOffsets(rootView: View, furiganaView: TextView,
                                         kanji: Pair<IntRange, Kanji?>): Pair<Float, Float> {
        // Get the text view bounds height
        val bounds = Rect()
        furiganaView.paint.getTextBounds(kanji.second!!.reading, 0, kanji.second!!.reading.length, bounds)
        val furiganaHeight = bounds.height()
        val furiganaWidth = bounds.width()

        // Get the layout of the text view
        val textViewLayout = rootView.detail_extract.layout
        // Get the line number of the current kanji
        val lineNumber = textViewLayout.getLineForOffset(kanji.first.first)
        // Get the top of the line (y coordinates) + the padding added to the text views to fit
        // top furigana
        val startYCoordinates =
                textViewLayout.getLineTop(lineNumber) + rootView.detail_extract.paddingTop +
                        rootView.detail_title.measuredHeight + rootView.detail_title.layout.topPadding +
                        rootView.detail_title.layout.bottomPadding
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
        var offsetX = startXCoordinates + ((endXCoordinates - startXCoordinates) / 2) - (furiganaWidth / 2.6)

        /*
         * Check if furigana is off screen.
         */
        // Get metrics of screen device.
        val displayMetrics = DisplayMetrics()
        this@DetailFragment.activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels

        // If the furigana goes off the end screen
        if (offsetX + furiganaWidth > screenWidth) {
            offsetX = (screenWidth - furiganaWidth - (rootView.detail_extract.paddingTop / 2)).toDouble()
        } else if (offsetX < 0) {
            offsetX = (screenWidth + (rootView.detail_extract.paddingTop / 2)).toDouble()
        }

        // Get the desired Y coordinate, a factor of the bounds height as a margin
        val offsetY = startYCoordinates - furiganaHeight - (furiganaHeight / 1.7)

        return Pair(offsetX.toFloat(), offsetY.toFloat())
    }

    override fun onDestroy() {
        JishoRepository.executor.shutdown()
        try {
            if (!JishoRepository.executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                JishoRepository.executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            JishoRepository.executor.shutdownNow()
        }

        super.onDestroy()
    }
}
