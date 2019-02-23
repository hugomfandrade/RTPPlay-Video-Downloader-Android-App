package org.hugoandrade.rtpplaydownloader.network

import android.support.v7.widget.RecyclerView
import android.databinding.DataBindingUtil
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.hugoandrade.rtpplaydownloader.R
import org.hugoandrade.rtpplaydownloader.databinding.DownloadItemBinding
import java.util.*

class DownloadItemsAdapter(private val listener: DownloadItemsAdapterListener) :

        RecyclerView.Adapter<DownloadItemsAdapter.ViewHolder>(), DownloadableItemStateChangeListener {

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

    inner class ViewHolder(val binding: DownloadItemBinding) : RecyclerView.ViewHolder(binding.getRoot()) {

        init {
            binding.root.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View) {
                    listener.onOn()
                }
            })
        }
    }

    interface DownloadItemsAdapterListener {
        fun onOn()
    }
}