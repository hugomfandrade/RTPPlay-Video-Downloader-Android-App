package org.hugoandrade.rtpplaydownloader.utils

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Handler
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import org.hugoandrade.rtpplaydownloader.R

class PermissionDialog(context: Context) {

    @Suppress("unused")
    private val TAG = PermissionDialog::class.java.simpleName

    private var mContext: Context = context

    private var mListener: OnPermissionListener? = null

    private var mAlertDialog: AlertDialog? = null
    private var mView: View? = null

    private var mHandler: Handler = Handler()

    init {
        buildView()
    }

    internal fun setOnPermissionDialogListener(listener: OnPermissionListener?) {
        mListener = listener
    }

    fun show() {
        val delayMillis = 100L
        mHandler.postDelayed({ delayedShow() }, delayMillis)
    }

    private fun buildView() {
        val view = View.inflate(mContext, R.layout.dialog_storage_permission, null)
        mView = view
        if (view == null) return
        view.findViewById<View>(R.id.tv_not_now).setOnClickListener {

            mListener?.onAllowed(false)

            dismissDialog()
        }
        view.findViewById<View>(R.id.tv_continue).setOnClickListener {

            mListener?.onAllowed(true)

            dismissDialog()
        }

        mAlertDialog = AlertDialog.Builder(mContext)
                .setOnKeyListener(DialogInterface.OnKeyListener { _, keyCode, event ->
                    if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {

                        mListener?.onAllowed(false)

                        return@OnKeyListener true
                    }
                    false
                })
                .create()
    }

    private fun delayedShow() {

        val window = mAlertDialog?.window

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window?.statusBarColor = Color.TRANSPARENT
        }

        val wm = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val size = Point()
        display.getSize(size)

        mAlertDialog?.setCanceledOnTouchOutside(false)

        mAlertDialog?.show()
        val v = mView
        if (v != null) {
            mAlertDialog?.setContentView(v)
        }
        val lp = WindowManager.LayoutParams()

        lp.copyFrom(window!!.attributes)
        //This makes the dialog take up the full width
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        lp.height = WindowManager.LayoutParams.MATCH_PARENT
        window.attributes = lp
    }

    private fun dismissDialog() {
        mAlertDialog!!.dismiss()
    }


    interface OnPermissionListener {
        fun onAllowed(wasAllowed: Boolean)
    }

    class Builder private constructor(context: Context) {

        private val P: CalendarDialogParams = CalendarDialogParams(context)

        fun setOnPermissionDialog(listener: OnPermissionListener): Builder {
            P.mOnPermissionListener = listener
            return this
        }

        fun create(): PermissionDialog {
            val permissionDialog = PermissionDialog(P.mContext)

            P.apply(permissionDialog)

            return permissionDialog
        }

        companion object {

            fun instance(context: Context): Builder {
                return Builder(context)
            }
        }
    }

    private class CalendarDialogParams internal constructor(internal var mContext: Context) {

        internal var mOnPermissionListener: OnPermissionListener? = null

        internal fun apply(permissionDialog: PermissionDialog) {
            permissionDialog.setOnPermissionDialogListener(mOnPermissionListener)
        }
    }
}