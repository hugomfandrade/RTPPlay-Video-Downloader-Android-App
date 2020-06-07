package org.hugoandrade.rtpplaydownloader.app.archive

import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.hugoandrade.rtpplaydownloader.R
import org.hugoandrade.rtpplaydownloader.databinding.DownloadItemBinding
import org.hugoandrade.rtpplaydownloader.network.DownloadableItem
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.hugoandrade.rtpplaydownloader.utils.ImageHolder
import java.io.File
import java.util.*

class ArchiveItemsAdapter : RecyclerView.Adapter<ArchiveItemsAdapter.ViewHolder>() {

    private val downloadableItemList: ArrayList<DownloadableItem> = ArrayList()

    private var listener: Listener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater: LayoutInflater = LayoutInflater.from(parent.context)
        val binding : DownloadItemBinding = DataBindingUtil.inflate(layoutInflater, R.layout.download_item, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val downloadableItem: DownloadableItem = downloadableItemList[position]

        if ((holder.binding.downloadItemTitleTextView as TextView).text.toString() != downloadableItem.filename) {
            (holder.binding.downloadItemTitleTextView as TextView).text = downloadableItem.filename
        }
        if (!holder.binding.downloadItemTitleTextView.isSelected) {
            holder.binding.downloadItemTitleTextView.isSelected = true
        }

        // THUMBNAIL

        val dir : File? = holder.itemView.context.getExternalFilesDir(null)
        val thumbnailUrl : String? = downloadableItem.thumbnailUrl

        ImageHolder.Builder()
                .withDefault(R.drawable.media_file_icon)
                .download(thumbnailUrl)
                .toDir(dir)
                .displayIn(holder.binding.downloadItemMediaImageView)

        when (downloadableItem.state) {
            DownloadableItem.State.Start -> {
                holder.binding.downloadItemTitleProgressView.setProgress(0.0)
                holder.binding.downloadProgressTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP,14f)
                holder.binding.downloadProgressTextView.text = ""
            }
            DownloadableItem.State.Downloading -> {
                downloadableItem.updateProgressUtils()

                holder.binding.downloadItemTitleProgressView.setProgress(downloadableItem.progress.toDouble())
                holder.binding.downloadProgressTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP,12f)
                holder.binding.downloadProgressTextView.text =
                        Math.round(downloadableItem.progress * 100f).toString() + "%"
                holder.binding.downloadProgressTextView.text =
                        MediaUtils.humanReadableByteCount(downloadableItem.progressSize, true) + "\\" +
                                MediaUtils.humanReadableByteCount(downloadableItem.filesize, true)
                holder.binding.downloadProgressTextView.text =
                        MediaUtils.humanReadableByteCount(downloadableItem.downloadingSpeed.toLong(), true) + "ps, " +
                                MediaUtils.humanReadableTime(downloadableItem.remainingTime)
            }
            DownloadableItem.State.End -> {
                holder.binding.downloadItemTitleProgressView.setProgress(1.0)
                holder.binding.downloadProgressTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP,12f)
                holder.binding.downloadProgressTextView.text = "100%"
                holder.binding.downloadProgressTextView.text = MediaUtils.humanReadableByteCount(downloadableItem.filesize, true)
            }
            DownloadableItem.State.Failed -> {
                holder.binding.downloadItemTitleProgressView.setProgress(0.0)
                holder.binding.downloadProgressTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP,14f)

                val message = downloadableItem.downloadMessage
                val context = holder.binding.downloadProgressTextView.context

                if (context != null && message != null) {
                    when (message) {
                        context.getString(R.string.url_no_longer_exists) -> {
                            holder.binding.downloadProgressTextView.setText(R.string.url_no_longer_exists)
                        }
                        context.getString(R.string.download_cancelled) -> {
                            holder.binding.downloadProgressTextView.setText(R.string.download_cancelled)
                        }
                        else -> {
                            holder.binding.downloadProgressTextView.setText(R.string.did_not_download)
                        }
                    }
                }
                else {
                    holder.binding.downloadProgressTextView.setText(R.string.did_not_download)
                }
            }
        }

        holder.binding.cancelDownloadImageView.visibility = View.GONE
        holder.binding.refreshDownloadImageView.visibility = View.GONE
        holder.binding.pauseDownloadImageView.visibility = View.GONE
        holder.binding.resumeDownloadImageView.visibility = View.GONE
    }

    fun get(index: Int): DownloadableItem {
        return downloadableItemList[index]
    }

    override fun getItemCount(): Int {
        return downloadableItemList.size
    }

    fun setItems(items: List<DownloadableItem>) {
        clear()
        addAll(items)
    }

    fun addAll(downloadableItems: List<DownloadableItem>) {
        for (downloadableItem in downloadableItems) {
            add(downloadableItem)
        }
    }

    fun add(downloadableItem: DownloadableItem) {
        val id = downloadableItem.id ?: -1
        synchronized(downloadableItemList) {

            if (!downloadableItemList.contains(downloadableItem)) {
                var pos = 0
                var posID = if (downloadableItemList.isEmpty()) 0 else downloadableItemList[pos].id ?: -1
                while(pos < downloadableItemList.size &&
                        id < posID) {
                    pos++
                    posID = downloadableItemList[pos].id ?: -1
                }
                downloadableItemList.add(pos, downloadableItem)
                notifyItemInserted(pos)
                notifyItemRangeChanged(pos, itemCount)
            }
        }
    }

    fun clear() {
        synchronized(downloadableItemList) {
            for (i in downloadableItemList.size - 1 downTo 0) {
                remove(downloadableItemList[i])
            }
        }
    }

    fun remove(downloadableItem: DownloadableItem) {
        synchronized(downloadableItemList) {
            if (downloadableItemList.contains(downloadableItem)) {
                val index: Int = downloadableItemList.indexOf(downloadableItem)
                downloadableItemList.remove(downloadableItem)
                notifyItemRemoved(index)
                notifyItemRangeChanged(index, itemCount)
            }
        }
    }

    interface Listener {
        fun onItemClicked(item : DownloadableItem)
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    inner class ViewHolder(val binding: DownloadItemBinding) :
            RecyclerView.ViewHolder(binding.root),
            View.OnClickListener {

        init {
            binding.root.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val item : DownloadableItem
            synchronized(downloadableItemList) {
                item = downloadableItemList[adapterPosition]
                listener?.onItemClicked(item)
            }
        }
    }
}