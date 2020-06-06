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

        fun requestPermission(activity: Activity, permission: String) {
            val locationPermission = hasGrantedPermission(activity, permission)

            // Permission has not been granted.
            if (!locationPermission) {
                requestPermission(
                        activity,
                        permission,
                        activity.findViewById<View>(android.R.id.content) as ViewGroup)
            }
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


        /**
         * Requests the fine location permission.
         * If the permission has been denied previously, a SnackBar
         * will prompt the user to grant the permission, otherwise
         * it is requested directly.
         */
        private fun requestPermission(activity: Activity,
                                      permission: String,
                                      layout: ViewGroup) {

            var shouldRequestPermission = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

            // Request anyway;
            shouldRequestPermission = false

            if (shouldRequestPermission) {
                // Provide an additional rationale to the user if the permission
                // was not granted and the user would benefit from additional
                // context for the use of the permission. For example if the user
                // has previously denied the permission.
                val snackbar = Snackbar.make(
                        layout,
                        R.string.write_external_storage_permission,
                        Snackbar.LENGTH_INDEFINITE)

                snackbar.setAction(
                        R.string.ok,
                        View.OnClickListener {
                            ActivityCompat.requestPermissions(
                                    activity,
                                    arrayOf(permission),
                                    REQUEST_WRITE_STORAGE)
                        })

                snackbar.show()

            } else {
                // Permission has not been granted yet. Request it directly.
                ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(permission),
                        REQUEST_WRITE_STORAGE)
            }
        }
    }
}