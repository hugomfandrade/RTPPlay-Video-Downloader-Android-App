package org.hugoandrade.rtpplaydownloader.app.main

import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.hugoandrade.rtpplaydownloader.DevConstants
import org.hugoandrade.rtpplaydownloader.R
import org.hugoandrade.rtpplaydownloader.databinding.DownloadItemBinding
import org.hugoandrade.rtpplaydownloader.network.DownloadableItem
import org.hugoandrade.rtpplaydownloader.network.DownloadableItemAction
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.hugoandrade.rtpplaydownloader.utils.ImageHolder
import java.io.File
import java.util.*

class DownloadItemsAdapter : RecyclerView.Adapter<DownloadItemsAdapter.ViewHolder>(), DownloadableItem.State.ChangeListener {

    private var recyclerView: RecyclerView? = null

    private val downloadableItemList: LinkedList<DownloadableItemAction> = LinkedList()

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        this.recyclerView = recyclerView

        startRefreshTimer()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)

        this.recyclerView = null

        stopRefreshTimer()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater: LayoutInflater = LayoutInflater.from(parent.context)
        val binding : DownloadItemBinding = DataBindingUtil.inflate(layoutInflater, R.layout.download_item, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val downloadableItemAction: DownloadableItemAction = downloadableItemList[position]
        val downloadableItem: DownloadableItem = downloadableItemAction.item

        val downloadItemTitleTextView = holder.binding.downloadItemTitleTextView as TextView
        if (downloadItemTitleTextView.text.toString() != downloadableItem.filename) {
            downloadItemTitleTextView.text = downloadableItem.filename
        }
        if (!downloadItemTitleTextView.isSelected) {
            downloadItemTitleTextView.isSelected = true
        }

        // THUMBNAIL

        val dir : File? = holder.itemView.context?.getExternalFilesDir(null)
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

        val isInDownloadingState : Boolean = downloadableItem.state == DownloadableItem.State.Downloading || downloadableItem.state == DownloadableItem.State.Start
        val isDownloading : Boolean = downloadableItemAction.isDownloading()

        holder.binding.cancelDownloadImageView.visibility = if (isInDownloadingState) View.VISIBLE else View.GONE
        holder.binding.refreshDownloadImageView.visibility = if (!isInDownloadingState) View.VISIBLE else View.GONE
        holder.binding.pauseDownloadImageView.visibility = if (DevConstants.enablePauseResume && isInDownloadingState && isDownloading) View.VISIBLE else View.GONE
        holder.binding.resumeDownloadImageView.visibility = if (DevConstants.enablePauseResume && isInDownloadingState && !isDownloading) View.VISIBLE else View.GONE
    }

    fun get(index: Int): DownloadableItemAction {
        return downloadableItemList[index]
    }

    override fun getItemCount(): Int {
        return downloadableItemList.size
    }

    fun set(items: List<DownloadableItemAction>) {
        clear()
        addAll(items)
    }

    fun addAll(downloadableItems: List<DownloadableItemAction>) {
        for (downloadableItem in downloadableItems) {
            doAdd(downloadableItem)
        }
        notifyDataSetChanged()
    }

    fun add(downloadableItem: DownloadableItemAction) {

        val pos = doAdd(downloadableItem);

        if (pos != -1) {
            notifyItemInserted(pos)
            notifyItemRangeChanged(pos, itemCount)
        }
    }

    fun clear() {
        for (i in downloadableItemList.size - 1 downTo 0) {
            doRemove(downloadableItemList[i])
        }
        notifyDataSetChanged()
    }

    fun remove(downloadableItem: DownloadableItemAction) {
        val index = doRemove(downloadableItem)

        if (index != -1) {
            notifyItemRemoved(index)
            notifyItemRangeChanged(index, itemCount)
        }
    }

    fun remove(downloadableItem: DownloadableItem) {
        findAction(downloadableItem)?.let { remove(it) }
    }

    private fun doAdd(downloadableItem: DownloadableItemAction): Int {

        if (!downloadableItemList.contains(downloadableItem)) {
            var pos = 0

            // put in right position
            val id = downloadableItem.item.id
            var currentID = if (downloadableItemList.isEmpty()) 0 else downloadableItemList[pos].item.id
            while(pos < downloadableItemList.size && id < currentID) {
                pos++
                if (pos >= downloadableItemList.size) break
                currentID = downloadableItemList[pos].item.id
            }

            downloadableItem.addDownloadStateChangeListener(this)
            downloadableItemList.add(pos, downloadableItem)
            return pos;
        }

        return -1
    }

    private fun doRemove(downloadableItem: DownloadableItemAction): Int {
        val index: Int = downloadableItemList.indexOf(downloadableItem)

        if (index != -1) {
            downloadableItem.removeDownloadStateChangeListener(this)
            downloadableItemList.remove(downloadableItem)
        }

        return index
    }

    private fun findAction(downloadableItem: DownloadableItem): DownloadableItemAction? {
        for (itemAction in downloadableItemList) {
            if (itemAction.item == downloadableItem) {
                return itemAction
            }
        }
        return null
    }

    override fun onDownloadStateChange(downloadableItem: DownloadableItem) {
        downloadableItemList.forEachIndexed { index, item ->
            if (item.item == downloadableItem) {
                recyclerView?.post {
                    notifyItemChanged(index)
                }
            }
        }
    }

    /**
     * Refresh Timer Support
     */

    companion object {
        const val REFRESH_WINDOW : Long = 1000
    }
    private val refreshTimerLock = Object()
    private var refreshTimer : Timer? = null

    private fun startRefreshTimer() {
        synchronized(refreshTimerLock) {
            refreshTimer?.cancel()
            refreshTimer = Timer("Refresh-Timer")
            refreshTimer?.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    synchronized(downloadableItemList) {
                        downloadableItemList.forEach { item ->
                            run {
                                if (item.item.state == DownloadableItem.State.Start ||
                                        item.item.state == DownloadableItem.State.Downloading) {
                                    item.item.fireDownloadStateChange()
                                }
                            }
                        }
                    }
                }
            }, REFRESH_WINDOW, REFRESH_WINDOW)
        }
    }

    private fun stopRefreshTimer() {
        synchronized(refreshTimerLock) {
            refreshTimer?.cancel()
            refreshTimer = null
        }
    }

    inner class ViewHolder(val binding: DownloadItemBinding) :
            RecyclerView.ViewHolder(binding.root),
            View.OnClickListener {

        init {
            binding.cancelDownloadImageView.setOnClickListener(this)
            binding.pauseDownloadImageView.setOnClickListener(this)
            binding.resumeDownloadImageView.setOnClickListener(this)
            binding.refreshDownloadImageView.setOnClickListener(this)
            binding.root.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val item : DownloadableItemAction
            synchronized(downloadableItemList) {
                item = downloadableItemList[adapterPosition]
            }
            when (v) {
                binding.cancelDownloadImageView -> item.cancel()
                binding.pauseDownloadImageView -> item.pause()
                binding.refreshDownloadImageView -> item.refresh()
                binding.resumeDownloadImageView -> item.resume()
                binding.root -> item.play()
            }
        }
    }
}