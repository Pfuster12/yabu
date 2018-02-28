package com.yabu.android.yabu.ui

import android.app.UiModeManager
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.SharedPreferences
import android.graphics.PorterDuff
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.yabu.android.yabu.R
import kotlinx.android.synthetic.main.fragment_user.view.*
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import jsondataclasses.Kanji
import viewmodel.ReviewViewModel

/**
 * User profile fragment, to show user profile and stats.
 */
class UserFragment : Fragment(), MainActivity.OnPageSelectedListener {

    private lateinit var mModel: ReviewViewModel

    // init the wikiExtracts list.
    private lateinit var mReviewKanjis: MutableList<Pair<Int, Kanji>>
    private lateinit var mRootView: View
    private lateinit var mPrefs: SharedPreferences

    // global line chart
    private lateinit var mLineChart: LineChart

    override fun onPageSelected(position: Int) {
        if (position == 0) {
            mModel.loadReviewKanjis(context)
            if (mLineChart != null) {
                mLineChart.invalidate()
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // start data load
        prepareViewModel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this.context)

        // init an empty list
        mReviewKanjis = mutableListOf()
    }

    /**
     * Override function for the onCreateView lifecycle of the activity.
     */
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Grab the root view inflated for the fragment.
        val rootView: View = inflater!!
                .inflate(R.layout.fragment_user, container, false)
        setToolbarTitle(rootView)

        mRootView = rootView

        setLineChart(rootView)
        setPieChart(rootView)
        // Return the inflated view to complete the onCreate process.
        return rootView
    }

    /**
     * Helper function to set toolbar to the User title.
     */
    private fun setToolbarTitle(rootView: View) {
        // Grab the title of the toolbar.
        val toolbarTitle: TextView = rootView.user_layout_toolbar.findViewById(R.id.toolbar_title)
        // Set the title of the toolbar to the Reading tab.
        toolbarTitle.text = getString(R.string.your_stats_page_title)

        val nightButton = rootView.user_layout_toolbar.findViewById<ImageView>(R.id.night_mode)
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
     * Helper fun to query review words from database and feed into recycler view adapter.
     */
    private fun prepareViewModel() {
        // Get the ViewModel for the Reading list
        mModel = ViewModelProviders.of(this@UserFragment)
                .get(ReviewViewModel::class.java)

        // Create the observer which updates the UI. Whenever the data changes, the new pojo list
        // is fed through and the UI can be updated.
        val observer: Observer<MutableList<Pair<Int, Kanji>>> = Observer { kanjis ->
            // Check for null since addAll() accepts only non null
            if (kanjis != null && kanjis.size != 0) {
                // clear the previous entries
                mReviewKanjis.clear()
                // Add the received wikiExtract list to the list hooked in the adapter.
                //mWikiExtracts.addAll(wikiExtracts)
                mReviewKanjis.addAll(kanjis)

                setPieChart(mRootView)
            } else {
                // Show no data message
            }
        }

        // Load the daily extracts from the main wiki page
        // through a retrofit call and set the LiveData value.
        mModel.loadReviewKanjis(context)

        // Observe the LiveData in the view model which will be set to the extracts,
        // passing in the fragment as the LifecycleOwner and the observer created above.
        mModel.reviewKanjis.observe(this@UserFragment, observer)
    }

    /**
     * Helper fun to set the chart data and format
     */
    private fun setLineChart(rootView: View) {
        mLineChart = rootView.chart
        val entries = mutableListOf<Entry>()

        // grab the articles as a list from monday to sunday.
        val articlesRead = getArticlesRead()

        var i = 0
        while (i < articlesRead.size) {
            // set entries to show in chart
            entries.add(Entry(i.toFloat(), articlesRead[i].toFloat()))
            i++
        }

        // the labels that should be drawn on the XAxis
        val quarters = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        // format the axis labels to be the days
        val formatter = object : IAxisValueFormatter {

            // we don't draw numbers, so no decimal digits needed
            val decimalDigits: Int
                get() = 0

            override fun getFormattedValue(value: Float, axis: AxisBase): String {
                return quarters[value.toInt()]
            }
        }

        // set data set
        val dataSet = LineDataSet(entries, "")
        dataSet.color = ContextCompat.getColor(context, R.color.colorAccent)
        dataSet.valueTextColor = ContextCompat.getColor(context, R.color.color300Grey)
        dataSet.fillColor = ContextCompat.getColor(context, R.color.colorAccent)
        dataSet.setDrawFilled(true)
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 3f
        dataSet.axisDependency = YAxis.AxisDependency.LEFT
        dataSet.setCircleColor(ContextCompat.getColor(context, R.color.colorAccent))
        dataSet.setCircleColorHole(ContextCompat.getColor(context, R.color.colorAccent))

        // create line data set
        val lineData = LineData(dataSet)
        mLineChart.data = lineData
        // no grid
        mLineChart.setDrawGridBackground(false)
        mLineChart.fitScreen()
        val des = Description()
        des.text = ""
        mLineChart.description = des
        mLineChart.legend.isEnabled = false

        // x axis format
        mLineChart.xAxis.valueFormatter = formatter
        mLineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        mLineChart.xAxis.setDrawGridLines(false)
        mLineChart.xAxis.granularity = 1f
        mLineChart.xAxis.textSize = 10f
        mLineChart.xAxis.textColor = ContextCompat.getColor(context, R.color.color700Grey)

        // y axis format
        mLineChart.axisLeft.setDrawGridLines(false)
        mLineChart.axisLeft.setDrawLabels(false)
        mLineChart.axisLeft.axisMinimum = 0f
        mLineChart.axisLeft.granularity = 1f
        mLineChart.axisLeft.textSize = 10f
        mLineChart.axisRight.isEnabled = false
        // refresh chart
        mLineChart.invalidate()
        mLineChart.animateX(1000, Easing.EasingOption.EaseOutBack)
    }

    /**
     * Helper fun to set the articles to persist for data graph in user fragment
     */
    private fun getArticlesRead(): List<Int> {
        // init the articles
        val articlesRead = mutableListOf<Int>()
        // cycle through the articles read to get all days
        var i = 1
        while (i < 8) {
            articlesRead.add(mPrefs.getInt(i.toString(), 0))
            i++
        }

        return articlesRead
    }

    /**
     * Helper fun to set the chart data and format
     */
    private fun setPieChart(rootView: View) {
        val chart = rootView.pie_chart
        val entries = mutableListOf<PieEntry>()
        var jlpt1Val = 0
        var jlpt2Val = 0
        var jlpt3Val = 0
        var jlpt4Val = 0
        var jlpt5Val = 0

        for (kanji in mReviewKanjis) {
            when (kanji.second.jlptTag) {
                -1 -> {}
                1 -> jlpt1Val++
                2 -> jlpt2Val++
                3 -> jlpt3Val++
                4 -> jlpt4Val++
                5 -> jlpt5Val++
            }
        }

        // set entries to show in chart
        entries.add(PieEntry(jlpt5Val.toFloat(), getString(R.string.JLPTN5)))
        entries.add(PieEntry(jlpt4Val.toFloat(), getString(R.string.JLPTN4)))
        entries.add(PieEntry(jlpt3Val.toFloat(), getString(R.string.JLPTN3)))
        entries.add(PieEntry(jlpt2Val.toFloat(), getString(R.string.JLPTN2)))
        entries.add(PieEntry(jlpt1Val.toFloat(), getString(R.string.JLPTN1)))
        // set data set
        val dataSet = PieDataSet(entries, "")
        dataSet.colors = mutableListOf(ContextCompat.getColor(context, R.color.colorJLPTN5),
                ContextCompat.getColor(context, R.color.colorJLPTN4),
                ContextCompat.getColor(context, R.color.colorJLPTN3),
                ContextCompat.getColor(context, R.color.colorJLPTN2),
                ContextCompat.getColor(context, R.color.colorJLPTN1))
        // create line data set
        val pieData = PieData(dataSet)
        chart.data = pieData

        // format the chart
        val des = Description()
        des.text = ""
        chart.description = des
        chart.maxAngle = 180f
        chart.holeRadius = 48f
        chart.rotationAngle = -240f
        chart.setDrawEntryLabels(false)
        chart.data.setValueTextColor(ContextCompat.getColor(context, R.color.colorBackground))
        chart.data.setValueTextSize(8f)
        chart.setNoDataText("No Data!")
        chart.setNoDataTextColor(ContextCompat.getColor(context, R.color.color500Grey))
        chart.setTransparentCircleAlpha(0)
        chart.setEntryLabelTextSize(8f)
        chart.legend.isEnabled = false
        chart.highlightValue(0f, 0)
        chart.invalidate()
    }
}
