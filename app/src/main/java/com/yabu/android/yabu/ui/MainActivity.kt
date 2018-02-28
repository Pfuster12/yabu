package com.yabu.android.yabu.ui

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import com.yabu.android.yabu.R
// Import the layout to avoid findView boilerplate
import  kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


/**
 * Main Activity holding ViewPager tabs of the main 3 fragments. This is the launch activity unless
 * it is first install, which would show StartUpActivity instead.
 */
class MainActivity : AppCompatActivity() {

    // The index of the 'primary' tab, i.e. the tab that will first show to the user
    // when the Main Activity is shown. Index 1 is the Reading Tab.
    private val primaryTab: Int = 1

    // late init a shared prefs var to check start up screen
    private lateinit var mPrefs: SharedPreferences

    companion object {
        // Executor variable to execute in worker threads
        lateinit var executor: ExecutorService

        // Preference key for night mode
        val NIGHT_MODE_KEY = "com.yabu.android.yabu.NIGHT_MODE"
    }

    lateinit var mListener: OnPageSelectedListener

    interface OnPageSelectedListener {
        fun onPageSelected(position: Int) {}
    }

    /**
     * Override function for the onCreate lifecycle of the activity.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // init a worker thread for the fragments
        // Executor variable to execute in worker threads
        executor = Executors.newCachedThreadPool()

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        checkNightMode()

        checkStartUpScreen()

        // Setup the TabLayout with the ViewPager with helper function.
        setupTabWithViewPager()
    }

    /**
     * check whether night mode is set and change the app.
     */
    private fun checkNightMode(): Boolean {
        // get boolean from preferences, if not found give false
        val isNightMode = mPrefs.getBoolean(MainActivity.NIGHT_MODE_KEY, false)

        // check if it has been shown
        if (isNightMode) {
            // if it is set the night mode
            val uiManager = this.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
            uiManager.nightMode = UiModeManager.MODE_NIGHT_YES
        } else {
            // if it isnt set the night mode off
            val uiManager = this.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
            uiManager.nightMode = UiModeManager.MODE_NIGHT_NO
        }

        return isNightMode
    }

    /**
     * Function to check for start up screen in shared preferences and show.
     */
    private fun checkStartUpScreen() {
        // get boolean from preferences, if not found give false
        val startUpScreenShown = mPrefs.getBoolean(StartUpActivity.START_UP_KEY, false)

        // check if it has been shown
        if (!startUpScreenShown) {
            // if not, show start up activity
            val intent = Intent(this, StartUpActivity::class.java)
            // start the activity
            startActivity(intent)
            // set the pref to shown to true
            val editor = mPrefs.edit()
            editor.putBoolean(StartUpActivity.START_UP_KEY, true)
            editor.apply()
        }
    }

    /**
     * Helper function to setup Tab Layout with ViewPager adapter in activity_main, and creates
     * and sets a tab selected listener to change icon colours to the primary color.
     */
    private fun setupTabWithViewPager() {
        val adapter = TabPagerAdapter(supportFragmentManager)
        // Set the adapter to the view pager in the xml layout, passing the support fragment manager.
        pager.adapter = adapter
        // Set the Tab layout with the ViewPager. tab_layout comes from the kotlinx import which
        // binds the views from the imported layout.
        tab_layout.setupWithViewPager(pager)
        // Make the current item shown be the Reading tab.
        pager.currentItem = primaryTab
        // Set tab icons with helper function.
        setTabLayout(tab_layout)
        // Implement the Tab listener.
        val tabListener = object : TabLayout.OnTabSelectedListener {
            /**
             * Override TabSelected to change the icon color on selected.
             */
            override fun onTabSelected(tab: TabLayout.Tab?) {
                // Set the icon color to the primary color on selection
                tab?.icon?.setTint(ContextCompat
                        .getColor(this@MainActivity, R.color.colorPrimary))
            }
            /**
             * Override TabReselected to change the icon color on reselected.
             */
            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Set the icon color to the primary color on selection
                tab?.icon?.setTint(ContextCompat
                        .getColor(this@MainActivity, R.color.colorPrimary))
            }
            /**
             * Override TabUnselected to change the icon color back.
             */
            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // Set the icon color to the primary color on selection
                tab?.icon?.setTint(ContextCompat
                        .getColor(this@MainActivity, R.color.colorTabUnselected))
            }
        }
        // Set the tab listener
        tab_layout.addOnTabSelectedListener(tabListener)

        pager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                mListener.onPageSelected(position)
            }
        })
    }

    /**
     * Helper function to set tab icons and tint color in the Tab Layout hooked to the Viewpager.
     */
    private fun setTabLayout(tabLayout: TabLayout) {
        // Set the tab icons within a loop
        for (i in 0..tabLayout.tabCount) {
            when (i) {
            // In the user tab set the user icon
                0 -> {
                    val tab: TabLayout.Tab? = tabLayout.getTabAt(0)
                    tab?.setIcon(R.drawable.ic_person_black_24dp)
                    tab?.icon?.setTint(ContextCompat.getColor(this, R.color.colorTabUnselected))
                }
            // In the Reading tab set the reading icon
                1 -> {
                    val tab: TabLayout.Tab? = tabLayout.getTabAt(1)
                    tab?.setIcon(R.drawable.ic_sort_black_24dp)
                    tab?.icon?.setTint(ContextCompat.getColor(this, R.color.colorPrimary))
                }
            // In the Saved Review words set the review icon
                2 -> {
                    val tab: TabLayout.Tab? = tabLayout.getTabAt(2)
                    tab?.setIcon(R.drawable.ic_folder_special_black_24dp)
                    tab?.icon?.setTint(ContextCompat.getColor(this, R.color.colorTabUnselected))
                }
            }
        }
    }

    /**
     * Adapter for the ViewPager. This adapter will feed the correct fragments to the pager when
     * swiping through the tabs. FragmentPagerAdapter was used instead of FragmentStatePagerAdapter,
     * since 3 screens is contained, and the Reading tab is expected to not reload.
     */
    class TabPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        // This is the total count of fragments in the ViewPager. The user, reading and review
        // fragments, i.e. 3 tabs.
        private val fragmentCount: Int = 3

        /**
         * Override function for adapter to get item according to position.
         */
        override fun getItem(position: Int): Fragment {
            // Return the correct fragment according to its position.
            return when (position) {
                // We are in the first tab aka User tab
                0 -> UserFragment()
                // We are in the middle tab aka Reading tab.
                1 -> ReadingFragment()
                // We are in the third tab aka Review Words tab
                2 -> ReviewWordsFragment()
                // Handle default cases, in which case return to main Reading tab.
                else -> ReadingFragment()
            }
        }

        /**
         * Override function to get total count. Count is arrived from global $fragmentCount val
         * set above.
         */
        override fun getCount(): Int {
            return fragmentCount
        }
    }

    /**
     * Shutdown worker threads appropriately
     */
    override fun onDestroy() {
        executor.shutdown()
        try {
            if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
        }
        super.onDestroy()
    }
}
