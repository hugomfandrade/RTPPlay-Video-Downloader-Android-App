package org.hugoandrade.rtpplaydownloader.network

import android.support.v7.widget.RecyclerView
import android.databinding.DataBindingUtil
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadItemsAdapter.ViewHolder {
        val layoutInflater: LayoutInflater = LayoutInflater.from(parent.context)
        val binding : DownloadItemBinding = DataBindingUtil.inflate(layoutInflater, R.layout.download_item, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DownloadItemsAdapter.ViewHolder, position: Int) {
        // holder.binding.setPost(downloadableItemList[position])
        val downloadableItem: DownloadableItem = downloadableItemList[position]

        holder.binding.downloadItemTitleTextView.text = downloadableItem.filename

        if (downloadableItem.state == DownloadableItem.State.Start) {
            holder.binding.downloadItemTitleProgressView.setProgress(0.0)
        }
        else if (downloadableItem.state == DownloadableItem.State.Downloading) {
            holder.binding.downloadItemTitleProgressView.setProgress(downloadableItem.progress.toDouble())
        }
        else if (downloadableItem.state == DownloadableItem.State.End) {
            holder.binding.downloadItemTitleProgressView.setProgress(1.0)
        }

        val isInDownloadingState : Boolean = downloadableItem.state == DownloadableItem.State.Downloading
        val isDownloading : Boolean = downloadableItem.isDownloading()

        holder.binding.cancelDownloadImageView.visibility = if (isDownloading) View.VISIBLE else View.GONE
        holder.binding.pauseDownloadImageView.visibility = if (isDownloading && isInDownloadingState) View.VISIBLE else View.GONE
        holder.binding.resumeDownloadImageView.visibility = if (isDownloading && !isInDownloadingState) View.VISIBLE else View.GONE
        holder.binding.refreshDownloadImageView.visibility = if (!isDownloading) View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int {
        return downloadableItemList.size
    }

    fun add(downloadableItem: DownloadableItem) {
        synchronized(downloadableItemList) {
            downloadableItem.addDownloadStateChangeListener(this);
            downloadableItemList.add(downloadableItem)
            notifyItemInserted(getItemCount() - 1)
        }
    }

    fun remove(downloadableItem: DownloadableItem) {
        downloadableItem.removeDownloadStateChangeListener(this);
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
            recyclerView?.post(Runnable {
                if (recyclerView == null) {
                    notifyItemChanged(index)
                }
                else {
                    val view : View? = null// recyclerView?.layoutManager?.findViewByPosition(index)

                    if (view == null) {

                        while (recyclerView?.isComputingLayout() == true) { }
                        notifyItemChanged(index)
                    }
                    else {
                        // update here
                        notifyItemChanged(index)
                    }
                }
            })

        }

    }

    inner class ViewHolder(val binding: DownloadItemBinding) :
            RecyclerView.ViewHolder(binding.getRoot()),
            View.OnClickListener {

        init {
            binding.cancelDownloadImageView.setOnClickListener(this)
            binding.pauseDownloadImageView.setOnClickListener(this)
            binding.resumeDownloadImageView.setOnClickListener(this)
            binding.refreshDownloadImageView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            synchronized(downloadableItemList) {
                if (v == binding.cancelDownloadImageView) {
                    downloadableItemList.get(adapterPosition).cancel()
                }
                else if (v == binding.pauseDownloadImageView) {
                    downloadableItemList.get(adapterPosition).pause()
                }
                else if (v == binding.refreshDownloadImageView) {
                    downloadableItemList.get(adapterPosition).refresh()
                }
                else if (v == binding.resumeDownloadImageView) {
                    downloadableItemList.get(adapterPosition).resume()
                }
            }
        }
    }
}