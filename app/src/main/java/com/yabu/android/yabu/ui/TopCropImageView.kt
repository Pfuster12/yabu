package com.yabu.android.yabu.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView

/**
 * Custom ImageView class that scales the image and sets the top of the image to the top of the
 * image view frame for better cropping.
 */
class TopCropImageView : ImageView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defIntStyle: Int) : super(context, attrs, defIntStyle)

    // Set the scale type to matrix to apply a matrix transformation in setFrame()
    init {
        scaleType = ScaleType.MATRIX
    }

    /**
     * Override fun to calculate the factor to scale and crop an image to its image view.
     * The scaling leaves the image in the top, giving a bottom crop only.
     */
    override fun setFrame(l: Int, t: Int, r: Int, b: Int): Boolean {
        val frameWidth = r - l
        val frameHeight = b - t

        var scaleFactor = 1f

        if (frameWidth > drawable.intrinsicWidth || frameHeight > drawable.intrinsicHeight) {
            val fitXScale = frameWidth / drawable.intrinsicWidth.toFloat()
            val fitYScale = frameHeight / drawable.intrinsicHeight.toFloat()

            scaleFactor = Math.max(fitXScale, fitYScale)
        }

        val matrix = imageMatrix
        matrix.setScale(scaleFactor, scaleFactor, 0f, 0f)
        imageMatrix = matrix

        return super.setFrame(l, t, r, b)
    }
}