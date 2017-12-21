package com.yabu.android.yabu.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.yabu.android.yabu.R
import kotlinx.android.synthetic.main.fragment_reading.view.*

/**
 * Review words fragment, to be paired with ViewPager for tab slide animations in Main Activity.
 * This fragment displays the words from the CursorLoader, saved by users into the review database.
 */
class ReviewWordsFragment : Fragment() {

    /**
     * Override function for the onCreateView lifecycle of the activity.
     */
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Grab the root view inflated for the fragment.
        val rootView: View = inflater!!
                .inflate(R.layout.fragment_review_words, container, false)

        setToolbarTitle(rootView)

        // Return the inflated view to complete the onCreate process.
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
        toolbarTitle.text = getString(R.string.review_words_page_title)
    }
}
