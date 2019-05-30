package org.hugoandrade.rtpplaydownloader.network.parsing

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.KeyEvent
import android.view.View
import org.hugoandrade.rtpplaydownloader.R
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTaskBase
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationParserTaskBase
import kotlin.collections.ArrayList

class ParsingDialog(val mContext: Context) {

    @Suppress("unused")
    private val TAG = ParsingDialog::class.java.simpleName

    private var mAlertDialog: AlertDialog? = null
    private var mView: View? = null
    private val mParsingItemsAdapter = ParsingItemsAdapter()

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
        mView?.findViewById<View>(R.id.tv_parse_entire_series)?.visibility = View.GONE
        mView?.findViewById<RecyclerView>(R.id.parsing_items)?.visibility = View.GONE
        mView?.findViewById<RecyclerView>(R.id.parsing_items)?.layoutManager = LinearLayoutManager(mContext)
        mView?.findViewById<RecyclerView>(R.id.parsing_items)?.adapter = mParsingItemsAdapter

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

    fun loading() {
        mView?.findViewById<View>(R.id.tv_download)?.visibility = View.GONE
        mView?.findViewById<View>(R.id.tv_parse_entire_series)?.visibility = View.GONE
        mView?.findViewById<View>(R.id.parsing_items)?.visibility = View.GONE

        mView?.findViewById<View>(R.id.parsing_progress_bar)?.visibility = View.VISIBLE
        mView?.findViewById<View>(R.id.tv_cancel)?.visibility = View.VISIBLE
    }

    fun showParsingResult(parsingData: ParsingData) {
        mView?.findViewById<View>(R.id.parsing_progress_bar)?.visibility = View.GONE
        mView?.findViewById<View>(R.id.tv_cancel)?.visibility = View.GONE
        mView?.findViewById<View>(R.id.parsing_items)?.visibility = View.VISIBLE

        val tasks : ArrayList<DownloaderTaskBase> = parsingData.tasks
        val paginationTask: PaginationParserTaskBase? = parsingData.paginationTask

        mParsingItemsAdapter.clear()
        mParsingItemsAdapter.addAll(tasks)
        mParsingItemsAdapter.notifyDataSetChanged()

        mView?.findViewById<View>(R.id.tv_download)?.visibility = View.VISIBLE
        mView?.findViewById<View>(R.id.tv_download)?.setOnClickListener {

            mListener?.onDownload(mParsingItemsAdapter.getSelectedTasks())

            dismissDialog()
        }

        if (paginationTask != null) {
            mView?.findViewById<View>(R.id.tv_parse_entire_series)?.visibility = View.VISIBLE
            mView?.findViewById<View>(R.id.tv_parse_entire_series)?.setOnClickListener {

                mListener?.onParseEntireSeries(paginationTask)
            }
        }
    }

    fun showPaginationResult(tasks: ArrayList<DownloaderTaskBase>) {

        mView?.findViewById<View>(R.id.parsing_progress_bar)?.visibility = View.GONE
        mView?.findViewById<View>(R.id.tv_cancel)?.visibility = View.GONE
        mView?.findViewById<View>(R.id.tv_parse_entire_series)?.visibility = View.GONE

        mView?.findViewById<View>(R.id.parsing_items)?.visibility = View.VISIBLE

        mParsingItemsAdapter.clear()
        mParsingItemsAdapter.addAll(tasks)
        mParsingItemsAdapter.notifyDataSetChanged()

        mView?.findViewById<View>(R.id.tv_download)?.visibility = View.VISIBLE
        mView?.findViewById<View>(R.id.tv_download)?.setOnClickListener {

            mListener?.onDownload(mParsingItemsAdapter.getSelectedTasks())

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
        fun onDownload(tasks : ArrayList<DownloaderTaskBase>)
        fun onParseEntireSeries(paginationTask : PaginationParserTaskBase)
    }

    class Builder private constructor(context: Context) {

        private val params: ParsingDialogParams = ParsingDialogParams(context)

        fun setOnParsingDialogListener(listener: OnParsingListener): Builder {
            params.mOnParsingListener = listener
            return this
        }

        fun create(): ParsingDialog {
            val parsingDialog = ParsingDialog(params.mContext)

            params.apply(parsingDialog)

            return parsingDialog
        }

        companion object {

            fun instance(context: Context): Builder {
                return Builder(context)
            }
        }
    }

    private class ParsingDialogParams internal constructor(internal var mContext: Context) {

        internal var mOnParsingListener: OnParsingListener? = null

        internal fun apply(parsingDialog: ParsingDialog) {
            parsingDialog.setOnParsingDialogListener(mOnParsingListener)
        }
    }
}