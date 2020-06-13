package org.hugoandrade.rtpplaydownloader.app.main

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialog
import org.hugoandrade.rtpplaydownloader.R
import org.hugoandrade.rtpplaydownloader.network.DownloadableItem
import org.hugoandrade.rtpplaydownloader.utils.ImageHolder
import java.io.File

class DownloadableItemDetailsDialog(context: Context, var mItem: DownloadableItem) : AppCompatDialog(context) {

    private var mListener: OnItemDetailsListener? = null

    init {
        setContentView(R.layout.dialog_item_details)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val isArchived = mItem.isArchived
        findViewById<View>(R.id.archive_image_view)?.visibility = if (isArchived == null || !isArchived) View.VISIBLE else View.GONE
        findViewById<View>(R.id.archive_image_view)?.setOnClickListener {

            mListener?.onArchive(mItem)

            dismiss()
        }
        findViewById<View>(R.id.original_uri_image_view)?.setOnClickListener {

            mListener?.onRedirect(mItem)

            dismiss()
        }
        findViewById<View>(R.id.folder_image_view)?.setOnClickListener {

            mListener?.onShowInFolder(mItem)

            dismiss()
        }
        findViewById<View>(R.id.tv_play)?.setOnClickListener {

            mListener?.onPlay(mItem)

            dismiss()
        }

        findViewById<TextView>(R.id.filename_text_view)?.text = mItem.filename
        findViewById<TextView>(R.id.original_uri_text_view)?.text = mItem.url

        val dir : File? = context.getExternalFilesDir(null)
        val thumbnailUrl : String? = mItem.thumbnailUrl
        val imageView = findViewById<ImageView>(R.id.item_thumbnail_image_view)

        if (imageView != null) {
            ImageHolder.Builder()
                    .withDefault(R.drawable.media_file_icon)
                    .download(thumbnailUrl)
                    .toDir(dir)
                    .displayIn(imageView)
        }

        setOnKeyListener(DialogInterface.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {

                mListener?.onCancelled()

                dismiss()

                return@OnKeyListener true
            }
            false
        })

        // make full screen
        val lp = WindowManager.LayoutParams()
        lp.copyFrom(window?.attributes)
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        window?.attributes = lp
    }

    internal fun setOnItemDetailsDialogListener(listener: OnItemDetailsListener?) {
        mListener = listener
    }

    fun show(item: DownloadableItem) {
        mItem = item

        findViewById<TextView>(R.id.filename_text_view)?.text = mItem.filename
        findViewById<TextView>(R.id.original_uri_text_view)?.text = mItem.url

        val dir : File? = context.getExternalFilesDir(null)
        val thumbnailUrl : String? = mItem.thumbnailUrl
        val imageView = findViewById<ImageView>(R.id.item_thumbnail_image_view)

        if (imageView != null) {
            ImageHolder.Builder()
                    .withDefault(R.drawable.media_file_icon)
                    .download(thumbnailUrl)
                    .toDir(dir)
                    .displayIn(imageView)
        }

        show()
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