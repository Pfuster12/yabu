package com.yabu.android.yabu.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.yabu.android.yabu.R
import viewmodel.WikiExtractsViewModel
import kotlinx.android.synthetic.main.fragment_reading.view.*
import pojos.WikiExtract

/**
 * Reading list fragment, to be paired with ViewPager for tab slide animations in Main Activity.
 * This fragment displays a list of articles extracted from the JsonUtils API.
 */
class ReadingFragment : Fragment() {

    // The ViewModel to instantiate it through the provider.
    private lateinit var mModel: WikiExtractsViewModel
    // The linear manager of the recycler view.
    private lateinit var mLinearLayoutManager: LinearLayoutManager
    // init the wikiExtracts list.
    private lateinit var mWikiExtracts: MutableList<WikiExtract>
    // init the adapter
    private lateinit var mAdapter: RecyclerViewAdapter

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
        // init the linear layout manager.
        mLinearLayoutManager = LinearLayoutManager(this@ReadingFragment.context)
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
        // init an empty list
        mWikiExtracts = mutableListOf(WikiExtract(1, "bab", "bub"))
        // Set the adapter for the recycler view with the empty list.
        mAdapter = RecyclerViewAdapter(mWikiExtracts, this@ReadingFragment.context)
        rootView.reading_recycler_view.adapter = mAdapter

        // Return the inflated view to complete the onCreate process.
        return rootView
    }

    /**
     * Helper function to init view model, data load and implement
     * observer of LiveData changes to the UI.
     */
    private fun prepareViewModel() {
        // Get the ViewModel for the Reading list
        mModel = ViewModelProviders.of(this@ReadingFragment)
                .get(WikiExtractsViewModel::class.java)

        // Create the observer which updates the UI. Whenever the data changes, the new pojo
        // is fed through and the UI can be updated.
        val observer: Observer<MutableList<WikiExtract>> = Observer { wikiExtracts ->
            // Check for null since addAll() accepts only non null
            if (wikiExtracts != null) {
                // Add the received wikiExtract list to the list hooked in the adapter.
                //mWikiExtracts.addAll(wikiExtracts)
                mWikiExtracts.addAll(wikiExtracts)
                mAdapter.notifyDataSetChanged()
            }
            //test_text_reading.text = wikiExtracts?.get(0)?.textExtract
        }

        // Load the test arrays and set the LiveData value.
        mModel.loadExtracts("Cheese")

        // Observe the LiveData, passing in the fragment as the LifecycleOwner and the observer.
        mModel.extracts.observe(this@ReadingFragment, observer)
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
    }
}
