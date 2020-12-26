package org.hugoandrade.rtpplaydownloader.app.main

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDialog
import org.hugoandrade.rtpplaydownloader.R
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingData
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationParserTask
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.ParsingTask


class ParsingDialog(context: Context): AppCompatDialog(context) {

    companion object {
        private val TAG = ParsingDialog::class.java.simpleName
    }

    private val mParsingItemsAdapter = ParsingItemsAdapter()

    private var mListener: OnParsingListener? = null

    init {
        setContentView(R.layout.dialog_parsing)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        findViewById<View>(R.id.tv_cancel)?.setOnClickListener {

            mListener?.onCancelled()

            dismiss()
        }

        findViewById<View>(R.id.tv_download)?.visibility = View.GONE
        findViewById<View>(R.id.tv_parse_entire_series)?.visibility = View.GONE
        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.parsing_items)?.visibility = View.GONE
        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.parsing_items)?.isNestedScrollingEnabled = false
        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.parsing_items)?.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.parsing_items)?.adapter = mParsingItemsAdapter

        setOnKeyListener(DialogInterface.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {

                mListener?.onCancelled()

                dismiss()

                return@OnKeyListener true
            }
            false
        })

        setCanceledOnTouchOutside(true)
        setOnDismissListener {
            mListener?.onCancelled()
        }

        // make full screen
        val lp = WindowManager.LayoutParams()
        lp.copyFrom(window?.attributes)
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        window?.attributes = lp
    }

    internal fun setOnParsingDialogListener(listener: OnParsingListener?) {
        mListener = listener
    }

    fun loading() {
        findViewById<View>(R.id.tv_download)?.visibility = View.GONE
        findViewById<View>(R.id.tv_parse_entire_series)?.visibility = View.GONE
        findViewById<View>(R.id.parsing_items)?.visibility = View.GONE

        findViewById<View>(R.id.parsing_progress_bar)?.visibility = View.VISIBLE
        findViewById<View>(R.id.tv_cancel)?.visibility = View.VISIBLE
    }

    fun loadingMore() {
        mParsingItemsAdapter.hideLoadMoreButton()
        mParsingItemsAdapter.showProgressBarView()
        findViewById<View>(R.id.tv_download)?.visibility = View.GONE
        findViewById<View>(R.id.tv_cancel)?.visibility = View.VISIBLE
    }

    fun showParsingResult(parsingData: ParsingData) {
        findViewById<View>(R.id.parsing_progress_bar)?.visibility = View.GONE
        findViewById<View>(R.id.tv_cancel)?.visibility = View.GONE
        findViewById<View>(R.id.parsing_items)?.visibility = View.VISIBLE

        val tasks : ArrayList<ParsingTask> = parsingData.tasks
        val paginationTask: PaginationParserTask? = parsingData.paginationTask

        if (tasks.isEmpty()) {
            dismiss()
            return
        }

        mParsingItemsAdapter.clear()
        mParsingItemsAdapter.addAll(tasks)
        mParsingItemsAdapter.notifyDataSetChanged()

        findViewById<View>(R.id.tv_download)?.visibility = View.VISIBLE
        findViewById<View>(R.id.tv_download)?.setOnClickListener {

            mListener?.onDownload(mParsingItemsAdapter.getSelectedTasks())

            dismiss()
        }

        if (paginationTask != null && !paginationTask.getPaginationComplete()) {
            findViewById<View>(R.id.tv_parse_entire_series)?.visibility = View.VISIBLE
            findViewById<View>(R.id.tv_parse_entire_series)?.setOnClickListener {

                mListener?.onParseEntireSeries(paginationTask)
            }
        }
    }

    fun showPaginationResult(paginationTask: PaginationParserTask, tasks: ArrayList<ParsingTask>) {

        if (tasks.isEmpty()) {
            dismiss()
            return
        }

        findViewById<View>(R.id.parsing_progress_bar)?.visibility = View.GONE
        findViewById<View>(R.id.tv_cancel)?.visibility = View.GONE
        findViewById<View>(R.id.parsing_items)?.visibility = View.VISIBLE

        mParsingItemsAdapter.hideProgressBarView()
        mParsingItemsAdapter.clear()

        showPaginationMoreResult(paginationTask, tasks)
    }

    fun showPaginationMoreResult(paginationTask: PaginationParserTask, tasks: ArrayList<ParsingTask>) {

        mParsingItemsAdapter.hideProgressBarView()
        mParsingItemsAdapter.addAllAndNotify(tasks)

        findViewById<View>(R.id.tv_cancel)?.visibility = View.GONE
        findViewById<View>(R.id.tv_download)?.visibility = View.VISIBLE
        findViewById<View>(R.id.tv_download)?.setOnClickListener {

            mListener?.onDownload(mParsingItemsAdapter.getSelectedTasks())

            dismiss()
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