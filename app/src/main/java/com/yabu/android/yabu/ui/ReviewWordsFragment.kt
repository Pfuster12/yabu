package com.yabu.android.yabu.ui

import android.app.UiModeManager
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.SharedPreferences
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.preference.PreferenceManager
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.yabu.android.yabu.R
import jsondataclasses.Kanji
import kotlinx.android.synthetic.main.fragment_reading.view.*
import kotlinx.android.synthetic.main.fragment_review_words.view.*
import kotlinx.android.synthetic.main.no_words.view.*
import org.parceler.Parcels
import viewmodel.ReviewViewModel

/**
 * Review words fragment, to be paired with ViewPager for tab slide animations in Main Activity.
 * This fragment displays the words from the CursorLoader, saved by users into the review database.
 */
class ReviewWordsFragment : Fragment(), MainActivity.OnPageSelectedListener {

    companion object {
        val REVIEW_KEY = "com.yabu.android.yabu.REVIEW_KEY"
    }

    private lateinit var mModel: ReviewViewModel

    // init the wikiExtracts list.
    private lateinit var mReviewKanjis: MutableList<Pair<Int, Kanji>>

    // The linear manager of the recycler view.
    private lateinit var mLinearLayoutManager: LinearLayoutManager
    // init the adapter
    private lateinit var mAdapterReview: ReviewRecyclerViewAdapter
    private lateinit var mRootView: View
    private lateinit var mSwipeRefreshLayout: SwipeRefreshLayout

    private lateinit var mPrefs: SharedPreferences

    override fun onPageSelectedReview(position: Int) {
        if (position == 2) {
            mModel.loadReviewKanjis(context)
        }
    }
    /**
     * Override function when activity is created to prepare and load data from the view model.
     * This will allow data load to be ready to display when the fragment appears to the user.
     */
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Get the query from the database
        prepareViewModel()

        (activity as MainActivity).mListener = this@ReviewWordsFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            // init an empty list
            mReviewKanjis = mutableListOf()
        } else {
            // if not grab the list from the saved inst state
            val reviewParcelable: Parcelable = savedInstanceState.getParcelable(ReadingFragment.WIKI_EXTRACTS_KEY)
            // set the global list to this
            mReviewKanjis = Parcels.unwrap(reviewParcelable)
        }

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this.context)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putParcelable(ReviewWordsFragment.REVIEW_KEY, Parcels.wrap(mReviewKanjis))

        super.onSaveInstanceState(outState)
    }

    /**
     * Override function for the onCreateView lifecycle of the activity.
     */
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Grab the root view inflated for the fragment.
        val rootView: View = inflater!!
                .inflate(R.layout.fragment_review_words, container, false)
        mRootView = rootView

        setToolbarTitle(rootView)

        // init the linear layout manager.
        mLinearLayoutManager = object : LinearLayoutManager(this@ReviewWordsFragment.context) {
            // override to support predictive animations aka, shows non screen elements in animation
            override fun supportsPredictiveItemAnimations(): Boolean {
                return true
            }
        }
        // Set the layout manager.
        rootView.review_recycler_view.layoutManager = mLinearLayoutManager

        // Set the adapter for the recycler view with the empty list, and declare the listener
        // function.
        mAdapterReview = ReviewRecyclerViewAdapter(mReviewKanjis,
                this@ReviewWordsFragment.context, { _ -> },{ isEmpty ->  run {
            if (isEmpty) {
                showNoWords(rootView)
            } else {
                showRecyclerView(rootView)
            }
        } }, mRootView)
        rootView.review_recycler_view.adapter = mAdapterReview
        rootView.review_recycler_view.itemAnimator = RecyclerViewAnimator()

        mSwipeRefreshLayout = rootView.review_swipe_refresh
        rootView.review_swipe_refresh.isEnabled = true
        val oldReviewWords = mReviewKanjis
        // Set the swipe refresh listener
        rootView.review_swipe_refresh.setOnRefreshListener {
            onRefresh(rootView, oldReviewWords)
        }

        // Return the inflated view to complete the onCreate process.
        return rootView
    }

    /**
     * Swipe refresh function helper
     */
    private fun onRefresh(rootView: View, oldReviewWords: MutableList<Pair<Int, Kanji>>) {
        mModel.loadReviewKanjis(context)
        showRecyclerView(rootView)

        // Post a delayed check whether articles are the same and notify.
        val handler = Handler()
        handler.postDelayed( {
            // set the refresh indicator to false
            if (mReviewKanjis.containsAll(oldReviewWords)) {
                mSwipeRefreshLayout.isRefreshing = false
                Snackbar.make(rootView.review_recycler_parent,
                        "Review words are updated.", Snackbar.LENGTH_SHORT).show()
            }
        }, 3000)
    }

    /**
     * Helper fun to query review words from database and feed into recycler view adapter.
     */
    private fun prepareViewModel() {
        // Get the ViewModel for the Reading list
        mModel = ViewModelProviders.of(this@ReviewWordsFragment)
                .get(ReviewViewModel::class.java)

        // Create the observer which updates the UI. Whenever the data changes, the new pojo list
        // is fed through and the UI can be updated.
        val observer: Observer<MutableList<Pair<Int, Kanji>>> = Observer { kanjis ->
            // Check for null since addAll() accepts only non null
            if (kanjis != null && kanjis.size != 0) {
                mSwipeRefreshLayout.isRefreshing = false
                // clear the previous entries
                mReviewKanjis.clear()
                showRecyclerView(mRootView)
                // Add the received wikiExtract list to the list hooked in the adapter.
                //mWikiExtracts.addAll(wikiExtracts)
                mReviewKanjis.addAll(kanjis)
                mAdapterReview.notifyDataSetChanged()
            } else {
                // Show message
                if (mReviewKanjis.size == 0) {
                    showNoWords(mRootView)
                }
            }
        }

        // Load the daily extracts from the main wiki page
        // through a retrofit call and set the LiveData value.
        mModel.loadReviewKanjis(context)

        // Observe the LiveData in the view model which will be set to the extracts,
        // passing in the fragment as the LifecycleOwner and the observer created above.
        mModel.reviewKanjis.observe(this@ReviewWordsFragment, observer)
    }

    private fun showNoWords(rootView: View) {
        rootView.review_recycler_view.visibility = View.GONE
        rootView.review_no_words.visibility = View.VISIBLE
        rootView.review_no_words.animate().alpha(1.0f).start()
    }


    private fun showRecyclerView(rootView: View) {
        rootView.review_no_words.visibility = View.GONE
        rootView.review_no_words.alpha = 0f
        rootView.review_recycler_view.visibility = View.VISIBLE
    }

    /**
     * Helper function to set toolbar to the Review Words title.
     */
    private fun setToolbarTitle(rootView: View) {
        // Grab the title of the toolbar.
        val toolbarTitle: TextView = rootView
                .review_layout_toolbar.findViewById(R.id.toolbar_title)
        // Set the title of the toolbar to the Reading tab.
        toolbarTitle.text = getString(R.string.review_words_page_title)


        val nightButton = rootView.review_layout_toolbar.findViewById<ImageView>(R.id.night_mode)
        nightButton.isSelected = checkNightMode()
        // set the night mode action button
        nightButton.setOnClickListener {
            if (nightButton.isSelected) {
                nightButton.isSelected = false
                nightButton.setColorFilter(
                        ContextCompat.getColor(context, R.color.color100Grey), PorterDuff.Mode.SRC_ATOP)
                setNightMode(false)
            } else {
                nightButton.isSelected = true
                nightButton.setColorFilter(
                        ContextCompat.getColor(context, R.color.colorTextWhite), PorterDuff.Mode.SRC_ATOP)
                setNightMode(true)
            }
        }
    }

    /**
     * check whether night mode is set and change the app.
     */
    private fun checkNightMode(): Boolean {
        // get boolean from preferences, if not found give false
        return mPrefs.getBoolean(MainActivity.NIGHT_MODE_KEY, false)
    }

    /**
     * set the night mode in the preferences.
     */
    private fun setNightMode(isNightMode: Boolean) {
        // set the pref to shown to true
        val editor = mPrefs.edit()
        editor.putBoolean(MainActivity.NIGHT_MODE_KEY, isNightMode)
        editor.apply()

        val uiManager = this.context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        // check if it has been shown
        if (isNightMode) {
            // if it is set the night mode
            uiManager.nightMode = UiModeManager.MODE_NIGHT_YES
        } else {
            // if it isnt set the night mode off
            uiManager.nightMode = UiModeManager.MODE_NIGHT_NO
        }
    }
}
