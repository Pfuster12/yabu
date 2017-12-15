package com.yabu.android.yabu.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toolbar
import com.yabu.android.yabu.R
import kotlinx.android.synthetic.main.fragment_reading.*
import kotlinx.android.synthetic.main.fragment_reading.view.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.toolbar.view.*

/**
 * Reading list fragment, to be paired with ViewPager for tab slide animations in Main Activity.
 * This fragment displays a list of articles extracted from the Wiki API.
 */
class ReadingFragment : Fragment() {

    /**
     * Override function for the onCreateView lifecycle of the activity.
     */
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Grab the root view inflated for the fragment.
        val rootView: View = inflater!!
                .inflate(R.layout.fragment_reading, container, false)
        // Grab the title of the toolbar.
        val toolbarTitle: TextView = rootView
                .layout_toolbar.findViewById<TextView>(R.id.toolbar_title)
        // Set the title of the toolbar to the Reading tab.
        toolbarTitle.text = getString(R.string.reading_page_title)
        // Return the inflated view to complete the onCreate process.
        return rootView
    }
}
