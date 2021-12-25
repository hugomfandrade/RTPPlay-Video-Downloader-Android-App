package org.hugoandrade.rtpplaydownloader.app.main

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDialog
import androidx.recyclerview.widget.RecyclerView
import org.hugoandrade.rtpplaydownloader.R
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingData
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingTaskResult
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationParserTask

class ParsingDialog(context: Context): AppCompatDialog(context) {

    companion object {
        @Suppress("unused") private val TAG = ParsingDialog::class.java.simpleName
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
        val recyclerView = findViewById<RecyclerView>(R.id.parsing_items)
        recyclerView?.visibility = View.GONE
        recyclerView?.isNestedScrollingEnabled = false
        recyclerView?.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        recyclerView?.adapter = mParsingItemsAdapter

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

    fun showParsingResult(parsingTaskResult: ParsingTaskResult) {
        findViewById<View>(R.id.parsing_progress_bar)?.visibility = View.GONE
        findViewById<View>(R.id.tv_cancel)?.visibility = View.GONE
        findViewById<View>(R.id.parsing_items)?.visibility = View.VISIBLE

        val parsingDatas : ArrayList<ParsingData> = parsingTaskResult.parsingDatas
        val paginationTask: PaginationParserTask? = parsingTaskResult.paginationTask

        if (parsingDatas.isEmpty()) {
            dismiss()
            return
        }

        mParsingItemsAdapter.clear()
        mParsingItemsAdapter.addAll(parsingDatas)
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

    fun showPaginationResult(paginationTask: PaginationParserTask, parsingDatas: ArrayList<ParsingData>) {

        if (parsingDatas.isEmpty()) {
            dismiss()
            return
        }

        findViewById<View>(R.id.parsing_progress_bar)?.visibility = View.GONE
        findViewById<View>(R.id.tv_cancel)?.visibility = View.GONE
        findViewById<View>(R.id.parsing_items)?.visibility = View.VISIBLE

        mParsingItemsAdapter.hideProgressBarView()
        mParsingItemsAdapter.clear()

        showPaginationMoreResult(paginationTask, parsingDatas)
    }

    fun showPaginationMoreResult(paginationTask: PaginationParserTask, parsingDatas: ArrayList<ParsingData>) {

        mParsingItemsAdapter.hideProgressBarView()
        mParsingItemsAdapter.addAllAndNotify(parsingDatas)

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
        fun onDownload(parsingDatas : ArrayList<ParsingData>)
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

    private class ParsingDialogParams(var mContext: Context) {

        var mOnParsingListener: OnParsingListener? = null

        fun apply(parsingDialog: ParsingDialog) {
            parsingDialog.setOnParsingDialogListener(mOnParsingListener)
        }
    }
}