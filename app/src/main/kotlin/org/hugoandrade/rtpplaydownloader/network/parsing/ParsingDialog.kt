package org.hugoandrade.rtpplaydownloader.network.parsing

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import org.hugoandrade.rtpplaydownloader.R
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTaskBase

class ParsingDialog(context: Context) {

    @Suppress("unused")
    private val TAG = ParsingDialog::class.java.simpleName

    private var mAlertDialog: AlertDialog? = null
    private var mView: View? = null
    private var mContext: Context = context
    private var mListener: OnParsingListener? = null

    private var mHandler: Handler = Handler()

    private var isDismissedBecauseOfTouchOutside : Boolean? = true

    init {
        buildView()
    }

    internal fun setOnParsingDialogListener(listener: OnParsingListener?) {
        mListener = listener
    }

    fun show() {
        val delayMillis = 100L
        mHandler.postDelayed({ delayedShow() }, delayMillis)
    }

    private fun buildView() {
        mView = View.inflate(mContext, R.layout.dialog_parsing, null)
        mView?.findViewById<View>(R.id.tv_cancel)?.setOnClickListener {

            mListener?.onCancelled()

            dismissDialog()
        }

        mView?.findViewById<View>(R.id.tv_download)?.visibility = View.GONE
        mView?.findViewById<View>(R.id.parsing_item_layout)?.visibility = View.GONE

        mAlertDialog = AlertDialog.Builder(mContext)
                .setOnKeyListener(DialogInterface.OnKeyListener { _, keyCode, event ->
                    if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {

                        mListener?.onCancelled()

                        dismissDialog()

                        return@OnKeyListener true
                    }
                    false
                })
                .create()
    }

    fun showParsingResult(task: DownloaderTaskBase?) {
        mView?.findViewById<View>(R.id.parsing_progress_bar)?.visibility = View.GONE
        mView?.findViewById<View>(R.id.tv_cancel)?.visibility = View.GONE

        mView?.findViewById<View>(R.id.parsing_item_layout)?.visibility = View.VISIBLE
        mView?.findViewById<TextView>(R.id.parsing_item_title_text_view)?.text = task?.videoFileName
        mView?.findViewById<TextView>(R.id.parsing_item_title_text_view)?.isSelected = true
        mView?.findViewById<View>(R.id.tv_download)?.visibility = View.VISIBLE
        mView?.findViewById<View>(R.id.tv_download)?.setOnClickListener {

            mListener?.onDownload(task)

            dismissDialog()
        }
    }

    private fun delayedShow() {

        if (checkNotNull(isDismissedBecauseOfTouchOutside == false)) {
            return
        }

        mAlertDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        mAlertDialog?.setCanceledOnTouchOutside(true)
        mAlertDialog?.show()
        mAlertDialog?.setContentView(checkNotNull(mView))
        mAlertDialog?.setOnDismissListener {
            if (checkNotNull(isDismissedBecauseOfTouchOutside)) {

                mListener?.onCancelled()
            }
        }
    }

    fun dismissDialog() {
        isDismissedBecauseOfTouchOutside = false
        mAlertDialog?.dismiss()
    }

    fun isShowing(): Boolean {
        return mAlertDialog?.isShowing ?: false
    }


    interface OnParsingListener {
        fun onCancelled()
        fun onDownload(task : DownloaderTaskBase?)
    }

    class Builder private constructor(context: Context) {

        private val P: CalendarDialogParams

        init {
            P = CalendarDialogParams(context)
        }

        fun setOnParsingDialog(listener: OnParsingListener): Builder {
            P.mOnParsingListener = listener
            return this
        }

        fun create(): ParsingDialog {
            val permissionDialog = ParsingDialog(P.mContext)

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

        internal var mOnParsingListener: OnParsingListener? = null

        internal fun apply(permissionDialog: ParsingDialog) {
            permissionDialog.setOnParsingDialogListener(mOnParsingListener)
        }
    }
}