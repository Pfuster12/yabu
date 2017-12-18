package com.yabu.android.yabu.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.yabu.android.yabu.R
import data.WikiExtractsViewModel
import kotlinx.android.synthetic.main.fragment_reading.*
import kotlinx.android.synthetic.main.fragment_reading.view.*
import pojos.WikiExtract

/**
 * Reading list fragment, to be paired with ViewPager for tab slide animations in Main Activity.
 * This fragment displays a list of articles extracted from the WikiUtils API.
 */
class ReadingFragment : Fragment() {

    private lateinit var mModel: WikiExtractsViewModel

    /**
     * Override function for the onCreateView lifecycle of the activity.
     */
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Grab the root view inflated for the fragment.
        val rootView: View = inflater!!
                .inflate(R.layout.fragment_reading, container, false)
        setToolbarTitle(rootView)

        // Get the ViewModel for the Reading list
        mModel = ViewModelProviders.of(this@ReadingFragment)
                .get(WikiExtractsViewModel::class.java)

        // Load the test arrays and set the LiveData value.
        mModel.loadExtracts()

        // Create the observer which updates the UI.
        val observer: Observer<Array<WikiExtract>> = Observer { extracts ->
            test_text_reading.text = extracts!![0].titleExtract
        }

        // Observe the LiveData, passing in the fragment as the LifecycleOwner and the observer.
        mModel.extracts.observe(this@ReadingFragment, observer)

        // Return the inflated view to complete the onCreate process.
        return rootView
    }

    private fun setToolbarTitle(rootView: View) {
        // Grab the title of the toolbar.
        val toolbarTitle: TextView = rootView
                .layout_toolbar.findViewById(R.id.toolbar_title)
        // Set the title of the toolbar to the Reading tab.
        toolbarTitle.text = getString(R.string.reading_page_title)
    }
}
