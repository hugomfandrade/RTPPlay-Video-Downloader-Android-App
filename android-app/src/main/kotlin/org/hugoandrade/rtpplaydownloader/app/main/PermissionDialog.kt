package org.hugoandrade.rtpplaydownloader.app.main

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatDialog
import org.hugoandrade.rtpplaydownloader.R

class PermissionDialog(context: Context) : AppCompatDialog(context) {

    companion object {
        private val TAG = PermissionDialog::class.java.simpleName
    }

    private var mListener: OnPermissionListener? = null

    init {
        setContentView(R.layout.dialog_storage_permission)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        findViewById<View>(R.id.tv_not_now)?.setOnClickListener {

            mListener?.onAllowed(false)

            dismiss()
        }
        findViewById<View>(R.id.tv_continue)?.setOnClickListener {

            mListener?.onAllowed(true)

            dismiss()
        }

        setOnKeyListener(DialogInterface.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {

                mListener?.onAllowed(false)

                return@OnKeyListener true
            }
            false
        })
    }

    internal fun setOnPermissionDialogListener(listener: OnPermissionListener?) {
        mListener = listener
    }

    interface OnPermissionListener {
        fun onAllowed(wasAllowed: Boolean)
    }

    class Builder private constructor(context: Context) {

        private val P: PermissionDialogParams = PermissionDialogParams(context)

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

    private class PermissionDialogParams internal constructor(internal var mContext: Context) {

        internal var mOnPermissionListener: OnPermissionListener? = null

        internal fun apply(permissionDialog: PermissionDialog) {
            permissionDialog.setOnPermissionDialogListener(mOnPermissionListener)
        }
    }
}