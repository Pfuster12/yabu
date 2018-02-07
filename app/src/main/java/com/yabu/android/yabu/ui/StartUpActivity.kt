package com.yabu.android.yabu.ui

import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.yabu.android.yabu.R
// Import the layout to avoid findView boilerplate
import kotlinx.android.synthetic.main.activity_start_up.*
import repository.JishoRepository
import java.net.URL
import java.util.logging.Logger

class StartUpActivity : AppCompatActivity() {

    /*
    Global variables defined in constants companion object.
     */
    companion object StartUpConstants{
        // Intent extra key for sign in boolean check.
        const val SIGN_IN_EXTRA: String = "com.yabu.android.yabu.SIGN_IN_EXTRA"
    }
    // Boolean to check whether user has signed in or not.
    private var isSigned: Boolean = false

    /**
     * Override function for the onCreate lifecycle of the activity.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_up)

        start_up_logo.animate().alpha(1f).setDuration(2000).start()

        start_up_description.animate().alpha(1f).setDuration(1000).setStartDelay(2000).start()

        start_up_sign_up_group.animate().alpha(1f).setStartDelay(4000).start()

        start_up_continue_no_sign.animate().alpha(1f).setStartDelay(4500).start()

        // Set the on click for continue without sign in option.
        start_up_continue_no_sign.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra(StartUpConstants.SIGN_IN_EXTRA, isSigned)
            startActivity(intent)
        }
    }
}
