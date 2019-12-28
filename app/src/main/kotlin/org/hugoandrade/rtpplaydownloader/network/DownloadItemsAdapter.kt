package org.hugoandrade.rtpplaydownloader.network

import android.databinding.DataBindingUtil
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.hugoandrade.rtpplaydownloader.DevConstants
import org.hugoandrade.rtpplaydownloader.R
import org.hugoandrade.rtpplaydownloader.databinding.DownloadItemBinding
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.hugoandrade.rtpplaydownloader.utils.ImageHolder
import java.io.File
import java.util.*

class DownloadItemsAdapter :

        RecyclerView.Adapter<DownloadItemsAdapter.ViewHolder>(),
        DownloadableItemState.ChangeListener {

    private val recyclerViewLock: Any = Object()
    private var recyclerView: RecyclerView? = null

    private val downloadableItemList: ArrayList<DownloadableItemAction> = ArrayList()

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        synchronized(recyclerViewLock) {
            this.recyclerView = recyclerView
        }

        startRefreshTimer()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)

        synchronized(recyclerViewLock) {
            this.recyclerView = null
        }

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

        if ((holder.binding.downloadItemTitleTextView as TextView).text.toString() != downloadableItem.filename) {
            (holder.binding.downloadItemTitleTextView as TextView).text = downloadableItem.filename
        }
        if (!holder.binding.downloadItemTitleTextView.isSelected) {
            holder.binding.downloadItemTitleTextView.isSelected = true
        }

        // THUMBNAIL

        val dir : File? = recyclerView?.context?.cacheDir
        val thumbnailUrl : String? = downloadableItem.thumbnailUrl

        ImageHolder.Builder()
                .withDefault(R.drawable.media_file_icon)
                .download(thumbnailUrl)
                .toDir(dir)
                .displayIn(holder.binding.downloadItemMediaImageView)

        when (downloadableItem.state) {
            DownloadableItemState.Start -> {
                holder.binding.downloadItemTitleProgressView.setProgress(0.0)
                holder.binding.downloadProgressTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP,14f)
                holder.binding.downloadProgressTextView.text = ""
            }
            DownloadableItemState.Downloading -> {
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
            DownloadableItemState.End -> {
                holder.binding.downloadItemTitleProgressView.setProgress(1.0)
                holder.binding.downloadProgressTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP,12f)
                holder.binding.downloadProgressTextView.text = "100%"
                holder.binding.downloadProgressTextView.text = MediaUtils.humanReadableByteCount(downloadableItem.filesize, true)
            }
            DownloadableItemState.Failed -> {
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

        val isInDownloadingState : Boolean = downloadableItem.state == DownloadableItemState.Downloading || downloadableItem.state == DownloadableItemState.Start
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

    fun addAll(downloadableItems: List<DownloadableItemAction>) {
        for (downloadableItem in downloadableItems) {
            add(downloadableItem)
        }
    }

    fun add(downloadableItem: DownloadableItemAction) {
        synchronized(downloadableItemList) {

            if (!downloadableItemList.contains(downloadableItem)) {
                var pos = 0
                while(pos < downloadableItemList.size &&
                        downloadableItem.item.id <
                        downloadableItemList[pos].item.id) {
                    pos++
                }
                downloadableItem.item.addDownloadStateChangeListener(this)
                downloadableItemList.add(pos, downloadableItem)
                notifyItemInserted(pos)
                notifyItemRangeChanged(pos, itemCount)
                recyclerView?.scrollToPosition(0)
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

    fun remove(downloadableItem: DownloadableItemAction) {
        synchronized(downloadableItemList) {
            downloadableItem.item.removeDownloadStateChangeListener(this)
            if (downloadableItemList.contains(downloadableItem)) {
                val index: Int = downloadableItemList.indexOf(downloadableItem)
                downloadableItemList.remove(downloadableItem)
                notifyItemRemoved(index)
                notifyItemRangeChanged(index, itemCount)
            }
        }
    }

    override fun onDownloadStateChange(downloadableItem: DownloadableItem) {
        synchronized(downloadableItemList) {
            downloadableItemList.forEachIndexed { index, item ->
                if (item.item == downloadableItem) {
                    internalNotifyItemChanged(index)
                }
            }
        }
    }

    private fun internalNotifyItemChanged(index: Int) {
        synchronized(recyclerViewLock) {
            recyclerView?.post {
                if (recyclerView == null) {
                    notifyItemChanged(index)
                } else {
                    val lm = recyclerView?.layoutManager

                    val v: View?
                    if (lm is LinearLayoutManager) {
                        val llm : LinearLayoutManager = lm
                        v = llm.findViewByPosition(index)
                    }
                    else {
                        v = null
                    }

                    if (v != null) {

                        val viewHolder : RecyclerView.ViewHolder? = recyclerView?.getChildViewHolder(v)

                        if (viewHolder is ViewHolder) {
                            val holder : ViewHolder = viewHolder
                            while (recyclerView?.isComputingLayout == true) { }

                            // var m = holder.hashCode().toString() + ": updating what was at (" + index.toString() + ") " + holder.binding.downloadItemTitleTextView.text.toString()
                            onBindViewHolder(holder, index)
                            // m = m + " and now is " + holder.binding.downloadItemTitleTextView.text.toString()
                            // android.util.Log.e(javaClass.simpleName, m)
                        } else {
                            // update here
                            // android.util.Log.e(javaClass.simpleName, "viewholder not found of $index")
                            notifyItemChanged(index)
                        }
                    } else {
                        // update here
                        // android.util.Log.e(javaClass.simpleName, "view not found of $index")
                        notifyItemChanged(index)
                    }
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
                    downloadableItemList.forEach { item ->
                        run {
                            if (item.item.state == DownloadableItemState.Start ||
                                item.item.state == DownloadableItemState.Downloading ) {
                                item.item.fireDownloadStateChange()
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