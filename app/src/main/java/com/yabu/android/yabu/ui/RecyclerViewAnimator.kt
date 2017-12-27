package com.yabu.android.yabu.ui

import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.RecyclerView
import android.view.animation.AnimationUtils

/**
 * Implementation of the Recycler View Default item animator. Override the add item animation
 * for a smoother fade in.
 */
class RecyclerViewAnimator : DefaultItemAnimator() {

    /**
     * Override the animate add to give it a fade in.
     */
    override fun animateAdd(holder: RecyclerView.ViewHolder?): Boolean {
        val shortAnimTime = holder?.itemView?.context?.resources
                ?.getInteger(android.R.integer.config_mediumAnimTime)
        holder?.itemView?.alpha = 0f
        holder?.itemView?.animate()
                ?.alpha(1f)
                ?.setDuration(shortAnimTime!!.toLong())
                ?.setListener(null)

        dispatchAddFinished(holder)
        return true
    }
}