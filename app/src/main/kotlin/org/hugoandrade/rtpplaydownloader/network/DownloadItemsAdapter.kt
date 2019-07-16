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
import org.hugoandrade.rtpplaydownloader.common.ImageHolder
import org.hugoandrade.rtpplaydownloader.databinding.DownloadItemBinding
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import java.util.*

class DownloadItemsAdapter :

        RecyclerView.Adapter<DownloadItemsAdapter.ViewHolder>(),
        DownloadableItemStateChangeListener {

    private val recyclerViewLock: Any = Object()
    private var recyclerView: RecyclerView? = null

    private val downloadableItemList: ArrayList<DownloadableItem> = ArrayList()

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        synchronized(recyclerViewLock) {
            this.recyclerView = recyclerView
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)

        synchronized(recyclerViewLock) {
            this.recyclerView = null
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater: LayoutInflater = LayoutInflater.from(parent.context)
        val binding : DownloadItemBinding = DataBindingUtil.inflate(layoutInflater, R.layout.download_item, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // holder.binding.setPost(downloadableItemList[position])
        val downloadableItem: DownloadableItem = downloadableItemList[position]

        if ((holder.binding.downloadItemTitleTextView as TextView).text.toString() != downloadableItem.filename) {
            (holder.binding.downloadItemTitleTextView as TextView).text = downloadableItem.filename
        }
        if (!holder.binding.downloadItemTitleTextView.isSelected) {
            holder.binding.downloadItemTitleTextView.isSelected = true
        }

        val thumbnailPath = downloadableItem.thumbnailPath
        if (thumbnailPath == null) {
            holder.binding.downloadItemMediaImageView.setImageResource(R.drawable.media_file_icon)
        }
        else {
            // holder.binding.downloadItemMediaImageView.setImageResource(R.drawable.media_file_icon)
            ImageHolder.Builder.instance(holder.binding.downloadItemMediaImageView)
                    .setFileUrl(thumbnailPath)
                    .setDefaultImageResource(R.drawable.media_file_icon)
                    .execute()
        }

        when {
            downloadableItem.state == DownloadableItemState.Start -> {
                holder.binding.downloadItemTitleProgressView.setProgress(0.0)
                holder.binding.downloadProgressTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP,14f)
                holder.binding.downloadProgressTextView.text = ""
            }
            downloadableItem.state == DownloadableItemState.Downloading -> {
                holder.binding.downloadItemTitleProgressView.setProgress(downloadableItem.progress.toDouble())
                holder.binding.downloadProgressTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP,12f)
                holder.binding.downloadProgressTextView.text = Math.round(downloadableItem.progress * 100f).toString() + "%"
                holder.binding.downloadProgressTextView.text =
                        MediaUtils.humanReadableByteCount(downloadableItem.progressSize, true) + "\\" +
                        MediaUtils.humanReadableByteCount(downloadableItem.fileSize, true)
                holder.binding.downloadProgressTextView.text =
                        MediaUtils.humanReadableByteCount(downloadableItem.downloadingSpeed.toLong(), true) + "ps, " +
                        MediaUtils.humanReadableTime(downloadableItem.remainingTime)
            }
            downloadableItem.state == DownloadableItemState.End -> {
                holder.binding.downloadItemTitleProgressView.setProgress(1.0)
                holder.binding.downloadProgressTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP,12f)
                holder.binding.downloadProgressTextView.text = "100%"
                holder.binding.downloadProgressTextView.text = MediaUtils.humanReadableByteCount(downloadableItem.fileSize, true)
            }
            downloadableItem.state == DownloadableItemState.Failed -> {
                holder.binding.downloadItemTitleProgressView.setProgress(0.0)
                holder.binding.downloadProgressTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP,14f)
                holder.binding.downloadProgressTextView.text = "did not download"
            }
        }

        val isInDownloadingState : Boolean = downloadableItem.state == DownloadableItemState.Downloading || downloadableItem.state == DownloadableItemState.Start
        val isDownloading : Boolean = downloadableItem.isDownloading()

        holder.binding.cancelDownloadImageView.visibility = if (isInDownloadingState) View.VISIBLE else View.GONE
        holder.binding.pauseDownloadImageView.visibility = if (DevConstants.enablePauseResume && isInDownloadingState && isDownloading) View.VISIBLE else View.GONE
        holder.binding.resumeDownloadImageView.visibility = if (DevConstants.enablePauseResume && isInDownloadingState && !isDownloading) View.VISIBLE else View.GONE
        holder.binding.refreshDownloadImageView.visibility = if (!isInDownloadingState) View.VISIBLE else View.GONE
    }

    fun get(index: Int): DownloadableItem {
        return downloadableItemList[index]
    }

    override fun getItemCount(): Int {
        return downloadableItemList.size
    }

    fun clear() {
        synchronized(downloadableItemList) {
            downloadableItemList.forEach(action = {remove(it)})
        }
    }

    fun addAll(downloadableItems: List<DownloadableItem>) {
        synchronized(downloadableItemList) {
            downloadableItems.forEach(action = {add(it)})
        }
    }

    fun add(downloadableItem: DownloadableItem) {
        synchronized(downloadableItemList) {
            if (!downloadableItemList.contains(downloadableItem)) {
                downloadableItem.addDownloadStateChangeListener(this)
                downloadableItemList.add(0, downloadableItem)
                notifyItemInserted(0)
                notifyItemRangeChanged(0, itemCount)
            }
        }
    }

    fun remove(downloadableItem: DownloadableItem) {
        downloadableItem.removeDownloadStateChangeListener(this)
        synchronized(downloadableItemList) {
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
            if (downloadableItemList.contains(downloadableItem)) {
                val index: Int = downloadableItemList.indexOf(downloadableItem)
                internalNotifyItemChanged(index)
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
            val item : DownloadableItem
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