package com.yabu.android.yabu.ui

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import com.yabu.android.yabu.R
import utils.BundleKeys

/**
 * Activity for the extract detail view, showing full text, word definition support and
 * image of extract in full. Functionality to record articles viewed for user stats is also added.
 */
class DetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        // Grab the parcelable extra passed by the intent.
        val parcelable: Parcelable = intent.extras.getParcelable(BundleKeys.WIKI_EXTRACTS_BUNDLE)

        // Grab the fragment manager and init a transaction.
        val fragmentTransaction = supportFragmentManager.beginTransaction()

        // Add to transaction and commit.
        fragmentTransaction
                .add(R.id.detail_fragment_container, DetailFragment.newInstance(parcelable))
                .commit()
    }
}
