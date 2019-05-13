package org.hugoandrade.rtpplaydownloader.network

import android.support.v7.widget.RecyclerView
import android.databinding.DataBindingUtil
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.hugoandrade.rtpplaydownloader.R
import org.hugoandrade.rtpplaydownloader.databinding.DownloadItemBinding
import java.util.*

class DownloadItemsAdapter() :

        RecyclerView.Adapter<DownloadItemsAdapter.ViewHolder>(),
        DownloadableItemStateChangeListener {

    private val recyclerViewLock: Any = Object()
    private var recyclerView: RecyclerView? = null

    private val downloadableItemList: ArrayList<DownloadableItem> = ArrayList<DownloadableItem>()

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

        (holder.binding.downloadItemTitleTextView as TextView).text = downloadableItem.filename
        holder.binding.downloadItemTitleTextView.isSelected = true;

        if (downloadableItem.state == DownloadableItemState.Start) {
            holder.binding.downloadItemTitleProgressView.setProgress(0.0)
            holder.binding.downloadProgressTextView.text = ""
        }
        else if (downloadableItem.state == DownloadableItemState.Downloading) {
            holder.binding.downloadItemTitleProgressView.setProgress(downloadableItem.progress.toDouble())
            holder.binding.downloadProgressTextView.text = Math.round(downloadableItem.progress * 100f).toString() + "%"
        }
        else if (downloadableItem.state == DownloadableItemState.End) {
            holder.binding.downloadItemTitleProgressView.setProgress(1.0)
            holder.binding.downloadProgressTextView.text = "100%"
        }
        else if (downloadableItem.state == DownloadableItemState.Failed) {
            holder.binding.downloadItemTitleProgressView.setProgress(0.0)
            holder.binding.downloadProgressTextView.text = "did not download"
        }

        val isInDownloadingState : Boolean = downloadableItem.state == DownloadableItemState.Downloading
        val isDownloading : Boolean = downloadableItem.isDownloading()

        holder.binding.cancelDownloadImageView.visibility = if (isInDownloadingState) View.VISIBLE else View.GONE
        holder.binding.pauseDownloadImageView.visibility = if (isInDownloadingState && isDownloading) View.VISIBLE else View.GONE
        holder.binding.resumeDownloadImageView.visibility = if (isInDownloadingState && !isDownloading) View.VISIBLE else View.GONE
        holder.binding.refreshDownloadImageView.visibility = if (!isInDownloadingState) View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int {
        return downloadableItemList.size
    }

    fun add(downloadableItem: DownloadableItem) {
        synchronized(downloadableItemList) {
            if (!downloadableItemList.contains(downloadableItem)) {
                downloadableItem.addDownloadStateChangeListener(this);
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
                    val view : View? = null// recyclerView?.layoutManager?.findViewByPosition(index)

                    if (view == null) {

                        while (recyclerView?.isComputingLayout() == true) { }
                        notifyItemChanged(index)
                    } else {
                        // update here
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
            val item : DownloadableItem;
            synchronized(downloadableItemList) {
                item = downloadableItemList[adapterPosition]
            }
            if (v == binding.cancelDownloadImageView) {
                item.cancel()
            }
            else if (v == binding.pauseDownloadImageView) {
                item.pause()
            }
            else if (v == binding.refreshDownloadImageView) {
                item.refresh()
            }
            else if (v == binding.resumeDownloadImageView) {
                item.resume()
            }
            else if (v == binding.root) {
                item.play()
            }
        }
    }
}