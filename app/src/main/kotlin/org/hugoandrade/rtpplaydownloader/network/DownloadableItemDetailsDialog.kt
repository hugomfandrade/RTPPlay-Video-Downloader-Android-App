package org.hugoandrade.rtpplaydownloader.network

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import org.hugoandrade.rtpplaydownloader.R
import org.hugoandrade.rtpplaydownloader.utils.ImageHolder
import java.io.File

class DownloadableItemDetailsDialog(val mContext: Context, var mItem: DownloadableItem) {

    private var mAlertDialog: AlertDialog? = null
    private var mView: View? = null

    private var mListener: OnItemDetailsListener? = null

    private var mHandler: Handler = Handler()

    init {
        buildView()
    }

    internal fun setOnItemDetailsDialogListener(listener: OnItemDetailsListener?) {
        mListener = listener
    }

    fun show() {
        val delayMillis = 100L
        mHandler.postDelayed(runnable, delayMillis)
    }

    private val runnable = Runnable { delayedShow() }

    private fun buildView() {
        mView = View.inflate(mContext, R.layout.dialog_item_details, null)

        val isArchived = mItem.isArchived
        mView?.findViewById<View>(R.id.archive_image_view)?.visibility = if (isArchived == null || !isArchived) View.VISIBLE else View.GONE
        mView?.findViewById<View>(R.id.archive_image_view)?.setOnClickListener {

            mListener?.onArchive(mItem)

            dismissDialog()
        }
        mView?.findViewById<View>(R.id.original_uri_image_view)?.setOnClickListener {

            mListener?.onRedirect(mItem)

            dismissDialog()
        }
        mView?.findViewById<View>(R.id.folder_image_view)?.setOnClickListener {

            mListener?.onShowInFolder(mItem)

            dismissDialog()
        }
        mView?.findViewById<View>(R.id.tv_play)?.setOnClickListener {

            mListener?.onPlay(mItem)

            dismissDialog()
        }

        mView?.findViewById<TextView>(R.id.filename_text_view)?.setText(mItem.filename)
        mView?.findViewById<TextView>(R.id.original_uri_text_view)?.setText(mItem.url)
        val dir : File? = mContext.getExternalFilesDir(null)
        val thumbnailUrl : String? = mItem.thumbnailUrl
        val imageView = mView?.findViewById<ImageView>(R.id.item_thumbnail_image_view)

        if (imageView != null) {
            ImageHolder.Builder()
                    .withDefault(R.drawable.media_file_icon)
                    .download(thumbnailUrl)
                    .toDir(dir)
                    .displayIn(imageView)
        }


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

    fun show(item: DownloadableItem) {
        mItem = item

        mView?.findViewById<TextView>(R.id.filename_text_view)?.setText(mItem.filename)
        mView?.findViewById<TextView>(R.id.original_uri_text_view)?.setText(mItem.url)
        val dir : File? = mContext.getExternalFilesDir(null)
        val thumbnailUrl : String? = mItem.thumbnailUrl
        val imageView = mView?.findViewById<ImageView>(R.id.item_thumbnail_image_view)

        if (imageView != null) {
            ImageHolder.Builder()
                    .withDefault(R.drawable.media_file_icon)
                    .download(thumbnailUrl)
                    .toDir(dir)
                    .displayIn(imageView)
        }
    }

    private fun delayedShow() {

        mAlertDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        mAlertDialog?.setCanceledOnTouchOutside(true)
        mAlertDialog?.show()
        mAlertDialog?.setContentView(checkNotNull(mView))
        mAlertDialog?.setOnDismissListener {
            mListener?.onCancelled()
        }
    }

    fun dismissDialog() {
        mHandler.removeCallbacks(runnable)
        mAlertDialog?.dismiss()
    }

    fun isShowing(): Boolean {
        return mAlertDialog?.isShowing ?: false
    }

    interface OnItemDetailsListener {
        fun onCancelled()
        fun onArchive(item: DownloadableItem)
        fun onRedirect(item: DownloadableItem)
        fun onShowInFolder(item: DownloadableItem)
        fun onPlay(item: DownloadableItem)
    }

    class Builder private constructor(context: Context) {

        private val params: ItemDetailsDialogParams = ItemDetailsDialogParams(context)

        fun setOnItemDetailsDialogListener(listener: OnItemDetailsListener): Builder {
            params.mOnItemDetailsListener = listener
            return this
        }

        fun create(item : DownloadableItem): DownloadableItemDetailsDialog {
            val itemDetailsDialog = DownloadableItemDetailsDialog(params.mContext, item)

            params.apply(itemDetailsDialog)

            return itemDetailsDialog
        }

        companion object {

            fun instance(context: Context): Builder {
                return Builder(context)
            }
        }
    }

    private class ItemDetailsDialogParams internal constructor(internal var mContext: Context) {

        internal var mOnItemDetailsListener: OnItemDetailsListener? = null

        internal fun apply(itemDetailsDialog: DownloadableItemDetailsDialog) {
            itemDetailsDialog.setOnItemDetailsDialogListener(mOnItemDetailsListener)
        }
    }
}