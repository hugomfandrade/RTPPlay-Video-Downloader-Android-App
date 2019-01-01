package org.hugoandrade.rtpplaydownloader.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.StringRes;
import android.support.annotation.UiThread;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.Toast;

import org.hugoandrade.rtpplaydownloader.DevConstants;
import org.hugoandrade.rtpplaydownloader.common.ExpandCollapseAnimation;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides some general utility helper methods.
 */
public final class ViewUtils {
    /**
     * Logging tag.
     */
    private static final String TAG = "ViewUtils";
    /**
     * Static string containing the last toast message that was displayed by
     * calling either of the showToast helper methods. This value is not
     * thread-safe and is only used by the Testing framework.
     */
    @SuppressWarnings("StaticNonFinalField")
    private static String sLastToast = null;

    /**
     * Ensure this class is only used as a utility.
     */
    private ViewUtils() {
        throw new AssertionError();
    }

    /**
     * Helper to show a short toast message.
     *
     * @param context activity context
     * @param text    string to display
     */
    @SuppressWarnings({"SameParameterValue", "unused"})
    @UiThread
    public static void showToast(Context context, String text, Object... args) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException(
                    "showToast requires a valid string");
        }

        if (args != null && args.length > 0) {
            text = String.format(text, args);
        }

        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();

        // Also duplicate Toast message in log file for debugging.
        if (DevConstants.showLog) Log.d(TAG, text);
        sLastToast = text;
    }

    /**
     * Helper to show a short toast message.
     *
     * @param context activity context
     * @param id      resource id of string to display
     */
    @UiThread
    public static void showToast(
            Context context,
            @StringRes int id,
            Object... args) {
        if (args != null && args.length > 0) {
            showToast(context, context.getString(id), args);
        } else {
            Toast.makeText(context, id, Toast.LENGTH_SHORT).show();
        }

        // Also duplicate Toast message in log file fro debugging.
        if (DevConstants.showLog) Log.d(TAG, context.getResources().getString(id));
        sLastToast = context.getResources().getString(id);
    }

    /**
     * Returns the last toast message displayed by calling either of the
     * showToast() helper methods. This method is only used by the UI testing
     * framework.
     */
    @SuppressWarnings("unused")
    @UiThread
    public static String getLastToast() {
        return sLastToast;
    }

    /**
     * Clears the last toast message displayed by calling either of the
     * showToast() helper methods. This method is only used by the UI testing
     * framework.
     */
    @SuppressWarnings("unused")
    @UiThread
    public static void clearLastToast() {
        sLastToast = null;
    }

    /**
     * Returns the display metrics for the provided context.
     *
     * @param context Any context.
     * @return DisplayMetrics instance.
     */
    public static DisplayMetrics getDisplayMetrics(Context context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();

        WindowManager windowManager =
                ((WindowManager) context.getSystemService(
                        Context.WINDOW_SERVICE));

        final Display defaultDisplay = windowManager.getDefaultDisplay();
        defaultDisplay.getMetrics(displayMetrics);

        return displayMetrics;
    }

    /**
     * Hides the soft keyboard for the provided view.
     *
     * @param view The target view for soft keyboard input.
     */
    public static void hideSoftKeyboard(View view) {
        InputMethodManager imm =
                (InputMethodManager) view.getContext().getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * Hides the soft keyboard for the provided view and clear focus.
     *
     * @param view The target view for soft keyboard input.
     */
    public static void hideSoftKeyboardAndClearFocus(View view) {
        view.clearFocus();
        hideSoftKeyboard(view);
    }

    /**
     * Hides the soft keyboard for the provided view.
     *
     * @param view The target view for soft keyboard input.
     */
    public static void showSoftKeyboard(View view) {
        InputMethodManager imm =
                (InputMethodManager) view.getContext().getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, 0);
    }

    /**
     * Hides the soft keyboard for the provided view and clear focus.
     *
     * @param view The target view for soft keyboard input.
     */
    public static void showSoftKeyboardAndRequestFocus(View view) {
        view.requestFocus();
        showSoftKeyboard(view);
    }

    /**
     * Finds all views with a specific tag.
     *
     * @param root The root view whose descendants are to be searched.
     * @param tag  The tag to search for.
     * @return A list of views that have the specified tag set.
     */
    public static List<View> findViewWithTagRecursively(
            ViewGroup root, Object tag) {
        List<View> allViews = new ArrayList<>();

        final int childCount = root.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View childView = root.getChildAt(i);

            if (childView instanceof ViewGroup) {
                allViews.addAll(
                        findViewWithTagRecursively((ViewGroup) childView, tag));
            } else {
                final Object tagView = childView.getTag();
                if (tagView != null && tagView.equals(tag)) {
                    allViews.add(childView);
                }
            }
        }

        return allViews;
    }

    /**
     * Find the first image view that has the specified transition name.
     *
     * @param root           The root view whose descendants are to be searched.
     *                       Works if the passed view is the match.
     * @param transitionName The transition name to search for.
     * @return The first image view with the specified transition name.
     */
    public static ImageView findImageViewWithTransitionName(
            View root, String transitionName) {
        // Be nice and check if the passed in view is an ImageView and the
        // just check its transition name and return.
        if (root instanceof ImageView) {
            String name = ViewCompat.getTransitionName(root);
            return TextUtils.equals(name, transitionName)
                   ? (ImageView) root
                   : null;
        } else if (root instanceof ViewGroup) {
            // Search all descendants for a match.
            ViewGroup viewGroup = (ViewGroup) root;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                final View childView = viewGroup.getChildAt(i);

                if (childView instanceof ViewGroup) {
                    ImageView view = findImageViewWithTransitionName(
                            childView, transitionName);
                    if (view != null) {
                        return view;
                    }
                } else if (childView instanceof ImageView) {
                    String name = ViewCompat.getTransitionName(childView);
                    if (TextUtils.equals(name, transitionName)) {
                        return (ImageView) childView;
                    }
                }
            }
        }

        if (DevConstants.showLog) Log.w(TAG,
              "findImageViewWithTransitionName: no image view with transition"
                      + " name "
                      + transitionName);

        return null;
    }


    public String getMethodName() {
        return Thread.currentThread().getStackTrace()[3].getMethodName();
    }

    /**
     * Show a toast message.
     */
    public static void showToast(Context context,
                                 String message) {
        Toast.makeText(context,
                message,
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Show a toast message.
     */
    public static void showSnackBar(View view,
                                    String message) {
        Snackbar.make(view,
                message,
                Snackbar.LENGTH_SHORT).show();
    }

    public static void setStatusBarColor(Activity activity, @ColorRes int colorRes) {
        Window window = activity.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.setStatusBarColor(activity.getResources().getColor(colorRes, null));
        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.setStatusBarColor(activity.getResources().getColor(colorRes));
            }
        }
    }

    public static int getOuterHeight(View view) {
        return view.getHeight()
                + ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).topMargin +
                + ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).bottomMargin;
    }

    public static void setHeightDp(Context context, View view, int heightInDPs) {
        if (view == null || context == null) return;

        view.clearAnimation();

        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                heightInDPs,
                context.getResources().getDisplayMetrics());
        view.setLayoutParams(params);
    }

    public static void setHeightDpAnim(Context context, View view, int heightInDPs, long duration) {
        // android.util.Log.e(TAG, "setHeightDpAnim = ");

        if (view == null || context == null) return;
        // android.util.Log.e(TAG, "setHeightDpAnims");


        int height = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                heightInDPs,
                context.getResources().getDisplayMetrics());

        view.clearAnimation();
        view.requestLayout();
        Animation anim = new ExpandCollapseAnimation(view, height);
        anim.setDuration(duration);
        view.startAnimation(anim);
    }

    public static void setHeightDpAnim(Context context, View view, int heightInDPs) {
        //setHeightDp(context, view, heightInDPs);
        setHeightDpAnim(context, view, heightInDPs, 500);
    }

    public static int setAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    public static int getColor(Context context, int colorRes) {
        return context.getResources().getColor(colorRes);
    }
}

