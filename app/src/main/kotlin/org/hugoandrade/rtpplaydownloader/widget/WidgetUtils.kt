package org.hugoandrade.rtpplaydownloader.widget

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.util.TypedValue

class WidgetUtils
/**
 * Ensure this class is only used as a utility.
 */
private constructor() {


    init {
        throw AssertionError()
    }


    companion object {

        /*
         * Logging tag.
         */
        private val TAG = "WidgetUtils"


        fun getThemePrimaryDarkColor(context: Context): Int {
            val colorAttr: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                android.R.attr.colorPrimaryDark
            } else {
                //Get colorAccent defined for AppCompat
                context.resources.getIdentifier("colorPrimaryDark", "attr", context.packageName)
            }
            val outValue = TypedValue()
            context.theme.resolveAttribute(colorAttr, outValue, true)
            return outValue.data
        }

        fun convertDpToPixel(dp: Float, context: Context): Float {
            return dp * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
        }
    }
}