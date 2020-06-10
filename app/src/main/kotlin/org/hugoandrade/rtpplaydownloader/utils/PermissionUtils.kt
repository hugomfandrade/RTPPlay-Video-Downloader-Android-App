package org.hugoandrade.rtpplaydownloader.utils

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.google.android.material.snackbar.Snackbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.view.View
import android.view.ViewGroup
import org.hugoandrade.rtpplaydownloader.R

class PermissionUtils
/**
 * Ensure this class is only used as a utility.
 */
private constructor() {

    init {
        throw AssertionError()
    }

    interface OnRequestPermissionsResultCallback {
        fun onRequestPermissionsResult(permissionType: String, wasPermissionGranted: Boolean)
    }

    companion object {

        /**
         * String used in logging output.
         */
        private val TAG = PermissionUtils::class.java.simpleName

        /**
         * RequestListener ID used in permission request calls.
         */
        private const val REQUEST_WRITE_STORAGE = 1
        private const val REQUEST_CODE_DEFAULT = 2

        fun requestPermission(activity: Activity, permission: String) {

            val permissionGranted = hasGrantedPermission(activity, permission)

            if (permissionGranted) return

            if (permission == Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                ActivityCompat.requestPermissions(activity, arrayOf(permission), REQUEST_WRITE_STORAGE)
            }
            else {
                ActivityCompat.requestPermissions(activity, arrayOf(permission), REQUEST_CODE_DEFAULT)
            }
        }

        fun hasGrantedPermissionAndRequestIfNeeded(activity: Activity, permission: String): Boolean {

            val permissionGranted = hasGrantedPermission(activity, permission)

            // if it does not have permission
            if (!permissionGranted) {

                if (permission == Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                    // show dialog explaining why
                    PermissionDialog.Builder.instance(activity)
                            .setOnPermissionDialog(object : PermissionDialog.OnPermissionListener {
                                override fun onAllowed(wasAllowed: Boolean) {
                                    if (wasAllowed) {
                                        requestPermission(activity, permission)
                                    }
                                }
                            })
                            .create()
                            .show()
                }
                else {

                    var shouldRequestPermission = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

                    if (shouldRequestPermission) {
                        val layout = activity.findViewById<View>(android.R.id.content) as ViewGroup
                        Snackbar.make(layout, "Please enable permission '$permission'", Snackbar.LENGTH_LONG)
                                .setAction(R.string.ok) {
                                    requestPermission(activity, permission)
                                }
                                .show()
                    }
                    else {
                        requestPermission(activity, permission)
                    }
                }
            }
            return permissionGranted
        }

        fun hasGrantedPermission(context: Context, permission: String): Boolean {
            return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

        /**
         * Helper method to be called from the activity's
         * onRequestPermissionResults() hook method which is called when a
         * permissions request has been completed.
         *
         * @return returns true if the permission is handled; `false` if not.
         */
        @TargetApi(Build.VERSION_CODES.M)
        fun onRequestPermissionsResult(
                activity: Activity,
                requestCode: Int,
                permissions: Array<String>,
                grantResults: IntArray,
                callback: OnRequestPermissionsResultCallback?): Boolean {

            if (requestCode == REQUEST_WRITE_STORAGE) {

                val layout = activity.findViewById<View>(android.R.id.content) as ViewGroup

                if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // Show granted replay message.
                    //Snackbar.make(layout, R.string.permission_available_fine_location, Snackbar.LENGTH_SHORT).show();

                    callback?.onRequestPermissionsResult(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            true)
                } else {
                    // Show denied replay message.
                    //Snackbar.make(layout, R.string.permissions_not_granted, Snackbar.LENGTH_SHORT).show();

                    callback?.onRequestPermissionsResult(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            false)
                }
                // Signal that we have handled the permissions.
                return true
            } else {
                // Signal that we did not handle the permissions.
                return false
            }
        }
    }
}