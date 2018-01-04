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

        // Set the on click for continue without sign in option.
        start_up_continue_no_sign.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra(StartUpConstants.SIGN_IN_EXTRA, isSigned)
            startActivity(intent)
        }
    }
}
