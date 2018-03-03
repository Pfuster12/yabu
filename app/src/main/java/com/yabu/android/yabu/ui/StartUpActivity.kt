package com.yabu.android.yabu.ui

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.yabu.android.yabu.R
// Import the layout to avoid findView boilerplate
import kotlinx.android.synthetic.main.activity_start_up.*
import android.support.v4.view.ViewCompat.getTransitionName
import android.util.Pair
import android.view.View
import android.view.Window
import android.view.Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME
import android.view.Window.STATUS_BAR_BACKGROUND_TRANSITION_NAME





class StartUpActivity : AppCompatActivity() {

    /*
    Global variables defined in constants companion object.
     */
    companion object StartUpConstants{
        // Intent extra key for sign in boolean check.
        const val START_UP_KEY: String = "com.yabu.android.yabu.SIGN_IN_EXTRA"
    }

    /**
     * Override function for the onCreate lifecycle of the activity.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_up)


        // Set the on click for continue without sign in option.
        start_up_continue_no_sign.setOnClickListener {
            val intent = Intent(this, InfoActivity::class.java)

            val statusBar: View = findViewById(android.R.id.statusBarBackground)
            val navigationBar: View = findViewById(android.R.id.navigationBarBackground)

            val pairs = ArrayList<Pair<View, String>>()
            if (statusBar != null) {
                pairs.add(android.util.Pair.create(statusBar, Window.STATUS_BAR_BACKGROUND_TRANSITION_NAME))
            }
            if (navigationBar != null) {
                pairs.add(Pair.create(navigationBar, Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME))
            }
            val options = ActivityOptions.makeSceneTransitionAnimation(this, pairs[0], pairs[1]).toBundle()

            startActivity(intent, options)
        }
    }

    /**
     * start the animation after oncreate
     */
    override fun onStart() {
        // set the animation
        val frameAnimation = animated_logo.drawable as AnimationDrawable
        frameAnimation.start()

        super.onStart()
    }

    override fun onResume() {
        super.onResume()
/*
        start_up_logo.animate().alpha(1f).setDuration(2000).start()

        start_up_description.animate().alpha(1f).setDuration(1000).setStartDelay(2000).start()

        start_up_continue_no_sign.animate().alpha(1f).setStartDelay(4000).start()*/
    }
}
