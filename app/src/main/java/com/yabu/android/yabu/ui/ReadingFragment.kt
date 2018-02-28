package com.yabu.android.yabu.ui

import android.app.UiModeManager
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.SharedPreferences
import android.graphics.PorterDuff
import android.net.ConnectivityManager
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
import com.bumptech.glide.Glide
import com.bumptech.glide.ListPreloader
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.google.firebase.analytics.FirebaseAnalytics
import com.yabu.android.yabu.R
import jsondataclasses.WikiExtract
import viewmodel.WikiExtractsViewModel
import kotlinx.android.synthetic.main.fragment_reading.view.*
import kotlinx.android.synthetic.main.no_connection.view.*
import org.parceler.Parcels
import repository.WikiExtractRepository
import java.util.concurrent.TimeUnit

/**
 * Reading list fragment, to be paired with ViewPager for tab slide animations in Main Activity.
 * This fragment displays a list of articles extracted from the JsonUtils API.
 */
class ReadingFragment : Fragment() {

    companion object {
        val WIKI_EXTRACTS_KEY = "com.yabu.android.yabu.WIKI_EXTRACTS_KEY"
    }

    // The ViewModel to instantiate it through the provider.
    private lateinit var mModel: WikiExtractsViewModel
    // The linear manager of the recycler view.
    private lateinit var mLinearLayoutManager: LinearLayoutManager
    // init the wikiExtracts list.
    private lateinit var mWikiExtracts: MutableList<WikiExtract>
    // init the adapter
    private lateinit var mAdapterReading: ReadingRecyclerViewAdapter
    // swipe refresh global var
    private lateinit var mSwipeRefreshLayout: SwipeRefreshLayout
    private lateinit var mRootView: View

    // late init a shared prefs var to check start up screen
    private lateinit var mPrefs: SharedPreferences

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    /**
     * Override function when activity is created to prepare and load data from the view model.
     * This will allow data load to be ready to display when the fragment appears to the user.
     */
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Set the view model and start data load as soon as the activity created.
        prepareViewModel()
    }

    /**
     * Override function for the onCreate lifecycle of the activity.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // init firebase analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this.context)

        if (savedInstanceState == null) {
            // init an empty list
            mWikiExtracts = mutableListOf()
        } else {
            // if not grab the list from the saved inst state
            val wikiParcelable: Parcelable = savedInstanceState.getParcelable(ReadingFragment.WIKI_EXTRACTS_KEY)
            // set the global list to this
            mWikiExtracts = Parcels.unwrap(wikiParcelable)
        }

        // init the linear layout manager.
        mLinearLayoutManager = LinearLayoutManager(this@ReadingFragment.context)
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this.context)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putParcelable(ReadingFragment.WIKI_EXTRACTS_KEY, Parcels.wrap(mWikiExtracts))

        super.onSaveInstanceState(outState)
    }

    /**
     * Override function for the onCreateView lifecycle of the activity.
     */
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Grab the root view inflated for the fragment.
        val rootView: View = inflater!!
                .inflate(R.layout.fragment_reading, container, false)

        // Set toolbar title.
        setToolbarTitle(rootView)

        // Set the layout manager.
        rootView.reading_recycler_view.layoutManager = mLinearLayoutManager

        // Add on scroll listener for glide integration to preload images before scrolling.
        rootView.reading_recycler_view.addOnScrollListener(prepareGlideRecyclerViewIntegration())
        // Set the adapter for the recycler view with the empty list, and declare the listener
        // function.
        mAdapterReading = ReadingRecyclerViewAdapter(mWikiExtracts,
                this@ReadingFragment.context, listener = { extract ->
            // log analytics event
            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, extract.title)
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)

            // check if its sw600 landscape layout
            if (rootView.landscape_detail_fragment_container != null) {
                // if it is add fragment to container
                val detailFragment = DetailFragment.newInstance(Parcels.wrap(extract))
                if (rootView.landscape_detail_fragment_container?.id != null) {
                    fragmentManager.beginTransaction()
                            .replace(rootView.landscape_detail_fragment_container?.id as Int, detailFragment)
                            .addToBackStack("DetailFragmentLandscape")
                            .commit()
                }
            } else {
                // we are in non sw600 landscape layout
                // set the function with the clicked extract parameter to start the framgent
                // bottom sheet. Put the extract as a parcelable.
                val bottomSheetFragment = DetailFragment.newInstance(Parcels.wrap(extract))
                bottomSheetFragment.show(fragmentManager, bottomSheetFragment.tag)
            }
        })
        rootView.reading_recycler_view.adapter = mAdapterReading
        rootView.reading_recycler_view.itemAnimator = RecyclerViewAnimator()

        // Show the recycler view layout
        showRecyclerView(rootView)

        mSwipeRefreshLayout = rootView.swipe_refresh
        if (rootView.landscape_detail_fragment_container == null) {
            rootView.swipe_refresh.isEnabled = true
            val oldExtracts = mWikiExtracts
            // Set the swipe refresh listener
            rootView.swipe_refresh.setOnRefreshListener {
                onRefresh(rootView, oldExtracts)
            }
        } else {
            mSwipeRefreshLayout.isEnabled = false
        }

        // assign a global var
        mRootView = rootView
        // Return the inflated view to complete the onCreate process.
        return rootView
    }

    /**
     * On refresh helper function for the swipe refresh layout
     */
    private fun onRefresh(rootView: View, oldExtracts: MutableList<WikiExtract>) {
        // Load the daily extracts from the main wiki page per user refresh
        mModel.loadExtracts(context)

        // Post a delayed check whether articles are the same and notify.
        val handler = Handler()
        handler.postDelayed(Runnable {
            // set the refresh indicator to false
            if (mWikiExtracts.containsAll(oldExtracts) && mWikiExtracts.size != 0) {
                mSwipeRefreshLayout.isRefreshing = false
                Snackbar.make(rootView.reading_recycler_parent,
                        "Articles already updated.", Snackbar.LENGTH_SHORT).show()
            } else if (mWikiExtracts.size == 0) {
                mSwipeRefreshLayout.isRefreshing = false
                Snackbar.make(rootView.reading_recycler_parent,
                        "No Articles loaded.", Snackbar.LENGTH_SHORT).show()
            }
        }, 3000)
    }

    /**
     * Helper function to init view model, data load and implement
     * observer of LiveData changes to the UI.
     */
    private fun prepareViewModel() {
        // Get the ViewModel for the Reading list
        mModel = ViewModelProviders.of(this@ReadingFragment)
                .get(WikiExtractsViewModel::class.java)

        // Create the observer which updates the UI. Whenever the data changes, the new pojo list
        // is fed through and the UI can be updated.
        val observer: Observer<MutableList<WikiExtract>> = Observer { wikiExtracts ->
            // Check for null since addAll() accepts only non null
            if (wikiExtracts != null && wikiExtracts.size != 0) {
                // Show the recycler view layout
                showRecyclerView(mRootView)
                mSwipeRefreshLayout.isRefreshing = false
                val oldExtracts = mWikiExtracts
                // clear the previous entries
                mWikiExtracts.clear()
                // Add the received wikiExtract list to the list hooked in the adapter.
                //mWikiExtracts.addAll(wikiExtracts)
                mWikiExtracts.addAll(wikiExtracts)
                mAdapterReading.notifyDataSetChanged()
                if (mWikiExtracts.containsAll(oldExtracts)) {
                    Snackbar.make(mRootView.reading_recycler_parent,
                            "Articles already updated.", Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(mRootView.reading_recycler_parent, "Articles updated.",
                            Snackbar.LENGTH_SHORT).show()
                }
            } else {
                // Show connection message
                if (mWikiExtracts.size == 0) {
                    showOnlyNoInternetConnection(mRootView)
                }
            }
        }

        // Load the daily extracts from the main wiki page
        // through a retrofit call and set the LiveData value.
        mModel.loadExtracts(context)

        // Observe the LiveData in the view model which will be set to the extracts,
        // passing in the fragment as the LifecycleOwner and the observer created above.
        mModel.extracts.observe(this@ReadingFragment, observer)
    }

    /**
     * Helper function to set up Glide's recycler view pre load image integration
     */
    private fun prepareGlideRecyclerViewIntegration(): RecyclerViewPreloader<String> {
        // Create a size provider which tells it its a set view to load
        val sizeProvider: ListPreloader.PreloadSizeProvider<String> = ViewPreloadSizeProvider<String>()
        // init a model provider to implement functions getting preload items
        val modelProvider: ListPreloader.PreloadModelProvider<String> =
                MyPreloadModelProvider(mWikiExtracts, this@ReadingFragment.context)
        // Return a preloader object.
        return RecyclerViewPreloader(Glide.with(this@ReadingFragment),
                        modelProvider,
                        sizeProvider,
                        4)
    }

    /**
     * Helper function to set toolbar to the Reading title.
     */
    private fun setToolbarTitle(rootView: View) {
        // Grab the title of the toolbar.
        val toolbarTitle: TextView = rootView
                .layout_toolbar.findViewById(R.id.toolbar_title)
        // Set the title of the toolbar to the Reading tab.
        toolbarTitle.text = getString(R.string.reading_page_title)

        val nightButton = rootView.layout_toolbar.findViewById<ImageView>(R.id.night_mode)
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

    /**
     * Helper fun to check internet connection whether wi-fi or mobile
     */
    private fun isConnected(): Boolean {
        val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val networkInfo = connMgr?.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    /**
     * Helper function to show the no connection message
     */
    private fun showOnlyNoInternetConnection(rootView: View) {
        rootView.reading_recycler_view.visibility = View.GONE
        rootView.reading_no_connection.visibility = View.VISIBLE
        rootView.reading_no_connection.reading_no_connection_detail
                .animate().alpha(1f).setDuration(2000).setStartDelay(1500).start()
    }

    /**
     * Helper function to show recycler view
     */
    private fun showRecyclerView(rootView: View) {
        rootView.reading_recycler_view.visibility = View.VISIBLE
        rootView.reading_no_connection.visibility = View.GONE
        rootView.reading_no_connection.reading_no_connection_detail.alpha = 0f
    }

    /**
     * Override on destroy to shutdown executor in repo
     */
    override fun onDestroy() {
        WikiExtractRepository.executor.shutdown()
        try {
            if (!WikiExtractRepository.executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                WikiExtractRepository.executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            WikiExtractRepository.executor.shutdownNow()
        }
        super.onDestroy()
    }

    /**
     * Preload Model provider for the Glide integration with Recycler View. The class receives
     * preload urls (models) to load when the user scrolls so the images are preloaded.
     */
    private class MyPreloadModelProvider(val wikiExtracts: MutableList<WikiExtract>, val context: Context)
        : ListPreloader.PreloadModelProvider<String> {

        /**
         * Gets the preload items from the urls
         */
        override fun getPreloadItems(position: Int): MutableList<String> {
            // Get the current extract.
            val currentExtract = position + 1
            // Check if position is within the extract positions.
            if (currentExtract < wikiExtracts.size + 1) {
                val extract = wikiExtracts[position]
                // Grab the thumbnail url of the current extract.
                val url = extract.thumbnail?.source
                if (url != null) {
                    return if (url.isBlank()) mutableListOf() else mutableListOf(url)
                }
            }
            // Return an empty list if not an extract.
            return mutableListOf()
        }

        /**
         * Returns a request builder that preload views will use to fill with image.
         */
        override fun getPreloadRequestBuilder(item: String?): RequestBuilder<*> {
            return GlideApp.with(context)
                    .load(item)
                    .placeholder(R.color.color500Grey)
                    .error(R.drawable.ground_astronautmhdpi)
                    .transition(withCrossFade())
        }
    }
}
