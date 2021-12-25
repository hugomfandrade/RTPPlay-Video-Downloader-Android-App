package org.hugoandrade.rtpplaydownloader.utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import com.google.android.material.snackbar.Snackbar
import androidx.core.view.ViewCompat
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import org.hugoandrade.rtpplaydownloader.R
import org.hugoandrade.rtpplaydownloader.dev.DevConstants
import org.hugoandrade.rtpplaydownloader.widget.ExpandCollapseAnimation
import java.util.*

class ViewUtils
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
        private val TAG = "ViewUtils"

        /**
         * Static string containing the last toast message that was displayed by
         * calling either of the showToast helper methods. This value is not
         * thread-safe and is only used by the Testing framework.
         */
        @SuppressWarnings("StaticNonFinalField")
        private var sLastToast: String? = null;

        /**
         * Helper to show a short toast message.
         *
         * @param context activity context
         * @param text    string to display
         */
        @UiThread
        fun showToast(context: Context, text: String?, vararg args: Any) {
            var text = text
            if (text == null || text.isEmpty()) {
                throw IllegalArgumentException(
                        "showToast requires a valid string")
            }

            if (args.size > 0) {
                text = String.format(text, *args)
            }

            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

            // Also duplicate Toast message in log file for debugging.
            if (DevConstants.showLog) Log.d(TAG, text)
            sLastToast = text
        }

        /**
         * Helper to show a short toast message.
         *
         * @param context activity context
         * @param id      resource id of string to display
         */
        @UiThread
        fun showToast(
                context: Context,
                @StringRes id: Int,
                vararg args: Any) {
            if (args.size > 0) {
                showToast(context, context.getString(id), *args)
            } else {
                Toast.makeText(context, id, Toast.LENGTH_SHORT).show()
            }

            // Also duplicate Toast message in log file fro debugging.
            if (DevConstants.showLog) Log.d(TAG, context.resources.getString(id))
            sLastToast = context.resources.getString(id)
        }

        /**
         * Returns the last toast message displayed by calling either of the
         * showToast() helper methods. This method is only used by the UI testing
         * framework.
         */
        @UiThread
        fun getLastToast(): String? {
            return sLastToast
        }

        /**
         * Clears the last toast message displayed by calling either of the
         * showToast() helper methods. This method is only used by the UI testing
         * framework.
         */
        @UiThread
        fun clearLastToast() {
            sLastToast = null
        }

        /**
         * Returns the display metrics for the provided context.
         *
         * @param context Any context.
         * @return DisplayMetrics instance.
         */
        fun getDisplayMetrics(context: Context): DisplayMetrics {
            val displayMetrics = DisplayMetrics()

            val windowManager = context.getSystemService(
                    Context.WINDOW_SERVICE) as WindowManager

            val defaultDisplay = windowManager.getDefaultDisplay()
            defaultDisplay.getMetrics(displayMetrics)

            return displayMetrics
        }

        /**
         * Hides the soft keyboard for the provided view.
         *
         * @param view The target view for soft keyboard input.
         */
        fun hideSoftKeyboard(view: View) {
            val imm = view.context.getSystemService(
                    Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }

        /**
         * Hides the soft keyboard for the provided view and clear focus.
         *
         * @param view The target view for soft keyboard input.
         */
        fun hideSoftKeyboardAndClearFocus(view: View?) {
            if (view == null) return
            view.clearFocus()
            hideSoftKeyboard(view)
        }

        /**
         * Hides the soft keyboard for the provided view.
         *
         * @param view The target view for soft keyboard input.
         */
        fun showSoftKeyboard(view: View) {
            val imm = view.context.getSystemService(
                    Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(view, 0)
        }

        /**
         * Hides the soft keyboard for the provided view and clear focus.
         *
         * @param view The target view for soft keyboard input.
         */
        fun showSoftKeyboardAndRequestFocus(view: View) {
            view.requestFocus()
            showSoftKeyboard(view)
        }

        /**
         * Finds all views with a specific tag.
         *
         * @param root The root view whose descendants are to be searched.
         * @param tag  The tag to search for.
         * @return A list of views that have the specified tag set.
         */
        fun findViewWithTagRecursively(
                root: ViewGroup, tag: Any?): List<View> {
            val allViews = ArrayList<View>()

            val childCount = root.childCount
            for (i in 0 until childCount) {
                val childView = root.getChildAt(i)

                if (childView is ViewGroup) {
                    allViews.addAll(
                            findViewWithTagRecursively(childView, tag))
                } else {
                    val tagView = childView.tag
                    if (tagView != null && tagView == tag) {
                        allViews.add(childView)
                    }
                }
            }

            return allViews
        }

        /**
         * Find the first image view that has the specified transition name.
         *
         * @param root           The root view whose descendants are to be searched.
         * Works if the passed view is the match.
         * @param transitionName The transition name to search for.
         * @return The first image view with the specified transition name.
         */
        fun findImageViewWithTransitionName(
                root: View, transitionName: String): ImageView? {
            // Be nice and check if the passed in view is an ImageView and the
            // just check its transition name and return.
            if (root is ImageView) {
                val name = ViewCompat.getTransitionName(root)
                return if (TextUtils.equals(name, transitionName))
                    root
                else
                    null
            } else if (root is ViewGroup) {
                // Search all descendants for a match.
                val viewGroup = root
                for (i in 0 until viewGroup.childCount) {
                    val childView = viewGroup.getChildAt(i)

                    if (childView is ViewGroup) {
                        val view = findImageViewWithTransitionName(
                                childView, transitionName)
                        if (view != null) {
                            return view
                        }
                    } else if (childView is ImageView) {
                        val name = ViewCompat.getTransitionName(childView)
                        if (TextUtils.equals(name, transitionName)) {
                            return childView
                        }
                    }
                }
            }

            if (DevConstants.showLog)
                Log.w(TAG,
                        "findImageViewWithTransitionName: no image view with transition"
                                + " name "
                                + transitionName)

            return null
        }


        fun getMethodName(): String {
            return Thread.currentThread().stackTrace[3].methodName
        }

        /**
         * Show a toast message.
         */
        fun showToast(context: Context?,
                      message: String?) {

            if (context != null && message != null) {
                Toast.makeText(context,
                        message,
                        Toast.LENGTH_SHORT).show()
            }
        }

        /**
         * Show a toast message.
         */
        fun showSnackBar(view: View,
                         message: String) {
            Snackbar.make(view,
                    message,
                    Snackbar.LENGTH_SHORT).show()
        }

        fun setStatusBarColor(activity: Activity, @ColorRes colorRes: Int) {
            val window = activity.window
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.statusBarColor = ContextCompat.getColor(activity, colorRes)
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    window.statusBarColor = ContextCompat.getColor(activity, colorRes)
                }
            }
        }

        fun getOuterHeight(view: View): Int {
            return (view.height
                    + (view.layoutParams as ViewGroup.MarginLayoutParams).topMargin +
                    +(view.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin)
        }

        fun setHeightDp(context: Context?, view: View?, heightInDPs: Int) {
            if (view == null || context == null) return

            view.clearAnimation()

            val params = view.layoutParams
            params.height = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    heightInDPs.toFloat(),
                    context.resources.displayMetrics).toInt()
            view.layoutParams = params
        }

        fun setHeightDpAnim(context: Context?, view: View?, heightInDPs: Int, duration: Long) {
            // android.util.Log.e(TAG, "setHeightDpAnim = ");

            if (view == null || context == null) return
            // android.util.Log.e(TAG, "setHeightDpAnims");


            val height = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    heightInDPs.toFloat(),
                    context.resources.displayMetrics).toInt()

            view.clearAnimation()
            view.requestLayout()
            val anim = ExpandCollapseAnimation(view, height)
            anim.duration = duration
            view.startAnimation(anim)
        }

        fun setHeightDpAnim(context: Context, view: View, heightInDPs: Int) {
            setHeightDpAnim(context, view, heightInDPs, 500)
        }

        fun setAlpha(color: Int, alpha: Int): Int {
            return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
        }

        fun getColor(context: Context, colorRes: Int): Int {
            return ContextCompat.getColor(context, colorRes)
        }

        fun isLandscape(context: Context): Boolean {
            return context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }

        fun isPortrait(context: Context): Boolean {
            return context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        }

        fun isTablet(context: Context): Boolean {
            return context.resources.getBoolean(R.bool.isTablet);
        }
    }
}