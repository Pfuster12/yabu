package com.yabu.android.yabu.ui

import android.animation.Animator
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.OverScroller
import com.yabu.android.yabu.R
import kotlinx.android.synthetic.main.activity_info.*
import android.animation.Animator.AnimatorListener
import android.animation.AnimatorSet
import android.animation.ObjectAnimator



class InfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.enterTransition = null
        setContentView(R.layout.activity_info)

        info_viewpager.adapter = InfoPagerAdapter(supportFragmentManager)

        dot_page_0_accent.visibility = View.VISIBLE
        info_viewpager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
                // Do nothing
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                // Do nothing
            }

            override fun onPageSelected(position: Int) {
                // Change the dots color to highlight the position
                when (position) {
                    0 -> {
                        dot_page_0_accent.visibility = View.VISIBLE
                        dot_page_1_accent.visibility = View.INVISIBLE
                        dot_page_2_accent.visibility = View.INVISIBLE
                        info_skip_text.text = getString(R.string.info_skip_text)
                    }
                    1 -> {
                        dot_page_0_accent.visibility = View.INVISIBLE
                        dot_page_1_accent.visibility = View.VISIBLE
                        dot_page_2_accent.visibility = View.INVISIBLE
                        info_skip_text.text = getString(R.string.info_skip_text)
                    }

                    2 -> {
                        dot_page_0_accent.visibility = View.INVISIBLE
                        dot_page_1_accent.visibility = View.INVISIBLE
                        dot_page_2_accent.visibility = View.VISIBLE
                        info_skip_text.text = getString(R.string.info_start_reading_text)
                    }
                }
            }
        })

        // set an on click to launch main activity
        info_skip_text.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        peekViewPagerAction()
    }

    private fun peekViewPagerAction() {
        val handler = Handler()
        handler.postDelayed(Runnable {
            // launch an object animator to scroll by x offset
            val x = 100
            val y = 0
            val xTranslate = ObjectAnimator.ofInt(info_viewpager, "scrollX", x)
            val yTranslate = ObjectAnimator.ofInt(info_viewpager, "scrollY", y)
            val animators = AnimatorSet()
            animators.duration = 500L
            animators.playTogether(xTranslate, yTranslate)
            animators.addListener(object : AnimatorListener {

                override fun onAnimationCancel(animation: Animator?) {
                    // Do nothing
                }

                override fun onAnimationStart(animation: Animator?) {
                    // Do nothing
                }

                override fun onAnimationRepeat(animation: Animator?) {
                    // Do nothing
                }

                // listen to the end to start the return animation
                override fun onAnimationEnd(arg0: Animator) {
                    val x2 = 0
                    val y2 = 0
                    val x2Translate = ObjectAnimator.ofInt(info_viewpager, "scrollX", x2)
                    val y2Translate = ObjectAnimator.ofInt(info_viewpager, "scrollY", y2)
                    val animators2 = AnimatorSet()
                    animators2.duration = 280L
                    animators2.playTogether(x2Translate, y2Translate)
                    animators2.start()

                }
            })
            animators.start()
        }, 900)
    }

    class InfoPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        override fun getItem(position: Int): Fragment {
            // Return the correct fragment according to its position.
            return when (position) {
            // We are in the first tab aka User tab
                0 -> Info1Fragment()
            // We are in the middle tab aka Reading tab.
                1 -> Info2Fragment()
            // We are in the third tab aka Review Words tab
                2 -> Info3Fragment()
            // Handle default cases, in which case return to main Reading tab.
                else -> Info1Fragment()
            }
        }

        override fun getCount(): Int {
            return 3
        }
    }
}
