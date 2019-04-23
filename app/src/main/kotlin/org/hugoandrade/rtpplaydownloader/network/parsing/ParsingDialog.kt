package org.hugoandrade.rtpplaydownloader.network.parsing

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
import android.widget.TextView
import org.hugoandrade.rtpplaydownloader.R
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTaskBase

class ParsingDialog(context: Context) {

    @Suppress("unused")
    private val TAG = ParsingDialog::class.java.simpleName

    private var mContext: Context = context

    private var mListener: OnParsingListener? = null

    private var mAlertDialog: AlertDialog? = null
    private var mView: View? = null

    private var mHandler: Handler = Handler()

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
        mView!!.findViewById<View>(R.id.tv_cancel).setOnClickListener {

            mListener!!.onCancelled()

            dismissDialog()
        }
        mView!!.findViewById<View>(R.id.tv_download).visibility = View.GONE
        mView!!.findViewById<View>(R.id.parsing_item_layout).visibility = View.GONE

        mAlertDialog = AlertDialog.Builder(mContext)
                .setOnKeyListener(DialogInterface.OnKeyListener { dialog, keyCode, event ->
                    if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {

                        mListener!!.onCancelled()

                        return@OnKeyListener true
                    }
                    false
                })
                .create()
    }

    fun showParsingResult(task: DownloaderTaskBase?) {
        mView!!.findViewById<View>(R.id.parsing_progress_bar).visibility = View.GONE
        mView!!.findViewById<View>(R.id.tv_cancel).visibility = View.GONE

        mView!!.findViewById<View>(R.id.parsing_item_layout).visibility = View.VISIBLE
        (mView!!.findViewById<View>(R.id.parsing_item_title_text_view) as TextView).text = task?.videoFileName
        mView!!.findViewById<View>(R.id.tv_download).visibility = View.VISIBLE
        mView!!.findViewById<View>(R.id.tv_download).setOnClickListener {

            mListener!!.onDownload(task)

            dismissDialog()
        }
    }

    private fun delayedShow() {

        mAlertDialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mAlertDialog!!.window!!.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            mAlertDialog!!.window!!.statusBarColor = Color.TRANSPARENT
        }

        val wm = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val size = Point()
        display.getSize(size)

        mAlertDialog!!.setCanceledOnTouchOutside(false)

        mAlertDialog!!.show()
        mAlertDialog!!.setContentView(mView!!)
        val lp = WindowManager.LayoutParams()
        val window = mAlertDialog!!.window

        lp.copyFrom(window!!.attributes)
        //This makes the dialog take up the full width
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        lp.height = WindowManager.LayoutParams.MATCH_PARENT
        window.attributes = lp
    }

    fun dismissDialog() {
        mAlertDialog!!.dismiss()
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