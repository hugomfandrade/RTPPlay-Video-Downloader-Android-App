package org.hugoandrade.rtpplaydownloader.app.main

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.view.KeyEvent
import android.view.View
import org.hugoandrade.rtpplaydownloader.R
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingData
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.ParsingTask
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationParserTask

class ParsingDialog(val mContext: Context) {

    @Suppress("unused")
    private val TAG = ParsingDialog::class.java.simpleName

    private var mAlertDialog: AlertDialog? = null
    private var mView: View? = null
    private val mParsingItemsAdapter = ParsingItemsAdapter()

    private var mListener: OnParsingListener? = null

    private var mHandler: Handler = Handler()
    private var isDelaying = false
    private val delayedShowCallback: Runnable = Runnable() {
        delayedShow()
        isDelaying = false
    }

    private var isDismissedBecauseOfTouchOutside : Boolean? = true

    init {
        buildView()
    }

    internal fun setOnParsingDialogListener(listener: OnParsingListener?) {
        mListener = listener
    }

    fun show() {
        val delayMillis = 100L
        isDelaying = true
        mHandler.postDelayed(delayedShowCallback, delayMillis)
    }

    private fun buildView() {
        mView = View.inflate(mContext, R.layout.dialog_parsing, null)
        mView?.findViewById<View>(R.id.tv_cancel)?.setOnClickListener {

            mListener?.onCancelled()

            dismissDialog()
        }

        mView?.findViewById<View>(R.id.tv_download)?.visibility = View.GONE
        mView?.findViewById<View>(R.id.tv_parse_entire_series)?.visibility = View.GONE
        mView?.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.parsing_items)?.visibility = View.GONE
        mView?.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.parsing_items)?.isNestedScrollingEnabled = false
        mView?.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.parsing_items)?.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(mContext)
        mView?.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.parsing_items)?.adapter = mParsingItemsAdapter

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

    fun loadingMore() {
        mParsingItemsAdapter.hideLoadMoreButton()
        mParsingItemsAdapter.showProgressBarView()
        mView?.findViewById<View>(R.id.tv_download)?.visibility = View.GONE
        mView?.findViewById<View>(R.id.tv_cancel)?.visibility = View.VISIBLE
    }

    fun showParsingResult(parsingData: ParsingData) {
        mView?.findViewById<View>(R.id.parsing_progress_bar)?.visibility = View.GONE
        mView?.findViewById<View>(R.id.tv_cancel)?.visibility = View.GONE
        mView?.findViewById<View>(R.id.parsing_items)?.visibility = View.VISIBLE

        val tasks : ArrayList<ParsingTask> = parsingData.tasks
        val paginationTask: PaginationParserTask? = parsingData.paginationTask

        if (tasks.isEmpty()) {
            dismissDialog()
            return
        }

        mParsingItemsAdapter.clear()
        mParsingItemsAdapter.addAll(tasks)
        mParsingItemsAdapter.notifyDataSetChanged()

        mView?.findViewById<View>(R.id.tv_download)?.visibility = View.VISIBLE
        mView?.findViewById<View>(R.id.tv_download)?.setOnClickListener {

            mListener?.onDownload(mParsingItemsAdapter.getSelectedTasks())

            dismissDialog()
        }

        if (paginationTask != null && !paginationTask.getPaginationComplete()) {
            mView?.findViewById<View>(R.id.tv_parse_entire_series)?.visibility = View.VISIBLE
            mView?.findViewById<View>(R.id.tv_parse_entire_series)?.setOnClickListener {

                mListener?.onParseEntireSeries(paginationTask)
            }
        }
    }

    fun showPaginationResult(paginationTask: PaginationParserTask,
                             tasks: ArrayList<ParsingTask>) {

        if (tasks.isEmpty()) {
            dismissDialog()
            return
        }

        mView?.findViewById<View>(R.id.parsing_progress_bar)?.visibility = View.GONE
        mView?.findViewById<View>(R.id.tv_cancel)?.visibility = View.GONE
        mView?.findViewById<View>(R.id.parsing_items)?.visibility = View.VISIBLE

        mParsingItemsAdapter.hideProgressBarView()
        mParsingItemsAdapter.clear()

        showPaginationMoreResult(paginationTask, tasks)
    }

    fun showPaginationMoreResult(paginationTask: PaginationParserTask,
                                 tasks: ArrayList<ParsingTask>) {

        mParsingItemsAdapter.hideProgressBarView()
        mParsingItemsAdapter.addAllAndNotify(tasks)

        mView?.findViewById<View>(R.id.tv_cancel)?.visibility = View.GONE
        mView?.findViewById<View>(R.id.tv_download)?.visibility = View.VISIBLE
        mView?.findViewById<View>(R.id.tv_download)?.setOnClickListener {

            mListener?.onDownload(mParsingItemsAdapter.getSelectedTasks())

            dismissDialog()
        }

        val show = !paginationTask.getPaginationComplete()

        if (show) {
            mParsingItemsAdapter.showLoadMoreButton()
            mParsingItemsAdapter.setListener(object : ParsingItemsAdapter.Listener {
                override fun onLoadMoreClicked() {
                    // load more
                    mListener?.onParseMore(paginationTask)
                }
            })
        }
        else {
            mParsingItemsAdapter.hideLoadMoreButton()
            mParsingItemsAdapter.setListener(null)
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
        mHandler.removeCallbacks(delayedShowCallback)
    }

    fun isShowing(): Boolean {
        return isDelaying || mAlertDialog?.isShowing ?: false
    }

    interface OnParsingListener {
        fun onCancelled()
        fun onDownload(tasks : ArrayList<ParsingTask>)
        fun onParseEntireSeries(paginationTask : PaginationParserTask)
        fun onParseMore(paginationTask: PaginationParserTask)
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