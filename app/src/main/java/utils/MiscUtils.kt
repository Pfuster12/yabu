package utils

import android.content.Context

/**
 * Miscellaneous utils to aid in code.
 */
class MiscUtils {

    companion object {
        fun getUtils(): MiscUtils {
            return MiscUtils()
        }
    }

    fun pxFromDp(context: Context, dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }
}