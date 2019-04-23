package org.hugoandrade.rtpplaydownloader.common

import android.app.Activity
import android.app.Fragment
import android.app.FragmentManager
import android.os.Bundle
import android.util.Log
import org.hugoandrade.rtpplaydownloader.DevConstants
import java.lang.ref.WeakReference
import java.util.HashMap


/**
 * Retains and manages state information between runtime configuration
 * changes to an Activity.  Plays the role of the "Originator" in the
 * Memento pattern.
 */
class RetainedFragmentManager
/**
 * Constructor initializes fields.
 */
(fragmentManager: FragmentManager,
 /**
  * Name used to identify the RetainedFragment.
  */
 private val mRetainedFragmentTag: String) {
    /**
     * Debugging tag used by the Android logger.
     */
    protected val TAG = javaClass.simpleName

    /**
     * WeakReference to the FragmentManager.
     */
    private val mFragmentManager: WeakReference<FragmentManager>

    /**
     * Reference to the RetainedFragment.
     */
    private var mRetainedFragment: RetainedFragment? = null

    /**
     * Return the Activity the RetainedFragment is attached to or null
     * if it's not currently attached.
     */
    val activity: Activity
        get() = mRetainedFragment!!.activity

    init {
        // Store a WeakReference to the Activity.
        mFragmentManager = WeakReference(fragmentManager)
    }// Store the tag used to identify the RetainedFragment.

    /**
     * Initializes the RetainedFragment the first time it's called.
     *
     * @returns true if it's first time the method's been called, else
     * false.
     */
    fun firstTimeIn(): Boolean {
        try {
            // Find the RetainedFragment on Activity restarts.  The
            // RetainedFragment has no UI so it must be referenced via
            // a tag.
            val tmpRetainedFragment : Fragment? = mFragmentManager.get()?.findFragmentByTag(mRetainedFragmentTag)

            // A value of null means it's the first time in, so there's
            // extra work to do.
            if (tmpRetainedFragment == null) {
                if (DevConstants.showLog)
                    Log.d(TAG,
                            "Creating new RetainedFragment $mRetainedFragmentTag")

                // Create a new RetainedFragment.
                mRetainedFragment = RetainedFragment()

                // Commit this RetainedFragment to the FragmentManager.
                mFragmentManager.get()?.
                        beginTransaction()?.
                        add(mRetainedFragment, mRetainedFragmentTag)?.
                        commit()
                return true
            } else {
                if (DevConstants.showLog)
                    Log.d(TAG,
                            "Returning existing RetainedFragment $mRetainedFragmentTag")
                return false
            }// A value of non-null means it's not first time in.
        } catch (e: NullPointerException) {
            if (DevConstants.showLog)
                Log.d(TAG,
                        "NPE in firstTimeIn()")
            return false
        }

    }

    /**
     * Add the @a object with the @a key.
     */
    fun put(key: String, `object`: Any) {
        mRetainedFragment!!.put(key, `object`)
    }

    /**
     * Add the @a object with its class name.
     */
    fun put(`object`: Any) {
        put(`object`.javaClass.name, `object`)
    }

    /**
     * Get the object with @a key.
     */
    operator fun <T> get(key: String): T {
        return mRetainedFragment!!.get<Any>(key) as T
    }

    /**
     * "Headless" Fragment that retains state information between
     * configuration changes.  Plays the role of the "Memento" in the
     * Memento pattern.
     */
    class RetainedFragment : Fragment() {
        /**
         * Maps keys to objects.
         */
        private val mData = HashMap<String, Any>()

        /**
         * Hook method called when a new instance of Fragment is
         * created.
         *
         * @param savedInstanceState
         * object that contains saved state information.
         */
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // Ensure the data survives runtime configuration changes.
            retainInstance = true
        }

        /**
         * Add the @a object with the @a key.
         */
        fun put(key: String, `object`: Any) {
            mData[key] = `object`
        }

        /**
         * Add the @a object with its class name.
         */
        fun put(`object`: Any) {
            put(`object`.javaClass.name, `object`)
        }

        /**
         * Get the object with @a key.
         */
        operator fun <T> get(key: String): T? {
            return mData[key] as T?
        }
    }
}
