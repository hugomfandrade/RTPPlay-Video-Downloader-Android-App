package org.hugoandrade.rtpplaydownloader.app.main

import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableBoolean
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import org.hugoandrade.rtpplaydownloader.R
import org.hugoandrade.rtpplaydownloader.databinding.ParsingItemBinding
import org.hugoandrade.rtpplaydownloader.databinding.ParsingItemLoadingBinding
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingData
import org.hugoandrade.rtpplaydownloader.utils.ImageHolder
import java.io.File
import kotlin.collections.ArrayList

class ParsingItemsAdapter : androidx.recyclerview.widget.RecyclerView.Adapter<ParsingItemsAdapter.ViewHolder>() {

    private var showLoadMore: Boolean = false
    private var showProgressBar: Boolean = false
    private val recyclerViewLock: Any = Object()
    private var recyclerView: androidx.recyclerview.widget.RecyclerView? = null

    private var listener: Listener? = null

    private val parsingItems: ArrayList<ParsingItem> = ArrayList()

    override fun onAttachedToRecyclerView(recyclerView: androidx.recyclerview.widget.RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        synchronized(recyclerViewLock) {
            this.recyclerView = recyclerView
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: androidx.recyclerview.widget.RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)

        synchronized(recyclerViewLock) {
            this.recyclerView = null
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater: LayoutInflater = LayoutInflater.from(parent.context)
        return if (viewType == 0) {
            val binding: ParsingItemBinding = DataBindingUtil.inflate(layoutInflater, R.layout.parsing_item, parent, false)
            ViewHolder(binding)
        }
        else  {
            val binding: ParsingItemLoadingBinding = DataBindingUtil.inflate(layoutInflater, R.layout.parsing_item_loading, parent, false)
            ViewHolder(binding)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == parsingItems.size) 1 else 0
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        if (holder.binding != null) {
            val parsingItem: ParsingItem = parsingItems[position]
            val parsingItemCount: Int = parsingItems.size
            holder.binding.item = parsingItem

            // THUMBNAIL

            val dir : File? = recyclerView?.context?.getExternalFilesDir(null)
            val thumbnailUrl : String? = parsingItem.parsingData.thumbnailUrl

            ImageHolder.Builder()
                    .withDefault(R.drawable.media_file_icon)
                    .download(thumbnailUrl)
                    .toDir(dir)
                    .displayIn(holder.binding.parsingItemMediaImageView)

            holder.binding.selectedCheckBox.visibility = if (parsingItemCount > 1) View.VISIBLE else View.GONE
        }

        if (holder.bindingLoading != null) {

            holder.bindingLoading.loadMoreButton.visibility = if (showLoadMore) View.VISIBLE else View.GONE
            holder.bindingLoading.progressBarView.visibility = if (showProgressBar) View.VISIBLE else View.GONE
        }

    }

    override fun getItemCount(): Int {
        return parsingItems.size + if (showLoadMore || showProgressBar) 1 else 0
    }

    fun add(parsingData: ParsingData) {
        synchronized(parsingItems) {
            for (parsingItem in parsingItems) {
                if (parsingItem.parsingData == parsingData) {
                    return
                }
            }
            parsingItems.add(ParsingItem(parsingData, ObservableBoolean(true)))
        }
    }

    fun addAndNotify(parsingData: ParsingData) {
        synchronized(parsingItems) {
            for (parsingItem in parsingItems) {
                if (parsingItem.parsingData == parsingData) {
                    return
                }
            }
            parsingItems.add(ParsingItem(parsingData, ObservableBoolean(true)))
            notifyItemInserted(parsingItems.size - 1)
            notifyItemRangeChanged(parsingItems.size - 1, parsingItems.size)
        }
    }

    fun remove(parsingData: ParsingData) {
        synchronized(parsingItems) {
            for ((index, parsingItem) in parsingItems.withIndex()) {
                if (parsingItem.parsingData == parsingData) {
                    parsingItems.removeAt(index)
                    notifyItemRemoved(index)
                    notifyItemRangeChanged(index, parsingItems.size)
                    return
                }
            }
        }
    }

    fun clear() {
        synchronized(parsingItems) {
            parsingItems.clear()
        }
    }

    fun addAllAndNotify(tasks: ArrayList<ParsingData>) {
        synchronized(parsingItems) {
            val prevItemCount: Int = itemCount
            tasks.forEach(action = { parsingData ->
                var alreadyInList = false
                for (parsingItem in parsingItems) {
                    if (parsingItem.parsingData == parsingData) {
                        alreadyInList = true
                        break
                    }
                }
                if (!alreadyInList) {
                    parsingItems.add(ParsingItem(parsingData, ObservableBoolean(true)))
                }
            })
            if (prevItemCount != itemCount) {
                notifyItemRangeChanged(prevItemCount, parsingItems.size)
            }
        }
    }

    fun addAll(tasks: ArrayList<ParsingData>) {
        synchronized(parsingItems) {
            tasks.forEach(action = { parsingData ->
                var alreadyInList = false
                for (parsingItem in parsingItems) {
                    if (parsingItem.parsingData == parsingData) {
                        alreadyInList = true
                        break
                    }
                }
                if (!alreadyInList) {
                    parsingItems.add(ParsingItem(parsingData, ObservableBoolean(true)))
                }
            })
        }
    }

    fun getSelectedTasks(): ArrayList<ParsingData> {
        synchronized(parsingItems) {
            val tasks : ArrayList<ParsingData> = ArrayList()
            for (parsingItem in parsingItems) {
                if (parsingItem.isSelected.get()) {
                    tasks.add(parsingItem.parsingData)
                }
            }
            return tasks
        }
    }

    fun showLoadMoreButton() {
        if (!showLoadMore) {
            val preItemCount = itemCount
            showLoadMore = true

            if (preItemCount != itemCount) {
                notifyItemInserted(itemCount)
                notifyItemRangeChanged(preItemCount, itemCount)
            }
            else {
                notifyItemChanged(itemCount)
            }
        }
    }

    fun hideLoadMoreButton() {
        if (showLoadMore) {
            val preItemCount = itemCount
            showLoadMore = false

            if (preItemCount != itemCount) {
                notifyItemRemoved(itemCount)
            }
            else {
                notifyItemChanged(itemCount)
            }
        }
    }

    fun showProgressBarView() {
        if (!showProgressBar) {
            val preItemCount = itemCount
            showProgressBar = true

            if (preItemCount != itemCount) {
                notifyItemInserted(itemCount)
                notifyItemRangeChanged(itemCount - 1, itemCount)
            }
            else {
                notifyItemChanged(itemCount)
            }
        }
    }

    fun hideProgressBarView() {
        if (showProgressBar) {
            val preItemCount = itemCount
            showProgressBar = false

            if (preItemCount != itemCount) {
                notifyItemRemoved(itemCount)
            }
            else {
                notifyItemChanged(itemCount)
            }
        }
    }

    interface Listener {
        fun onLoadMoreClicked()
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    inner class ViewHolder(val binding: ParsingItemBinding?,
                           val bindingLoading: ParsingItemLoadingBinding?) :

            androidx.recyclerview.widget.RecyclerView.ViewHolder(binding?.root?: bindingLoading?.root ?: View(null)),
            CompoundButton.OnCheckedChangeListener,
            View.OnClickListener {

        constructor(bindingLoading: ParsingItemLoadingBinding) :
            this(null, bindingLoading)

        constructor(binding: ParsingItemBinding):
                this(binding, null)

        override fun onClick(v: View?) {
            listener?.onLoadMoreClicked()
        }

        override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
            val item : ParsingItem
            synchronized(parsingItems) {
                item = parsingItems[adapterPosition]
                item.isSelected.set(isChecked)
            }
        }

        init {
            binding?.selectedCheckBox?.setOnCheckedChangeListener(this)
            bindingLoading?.loadMoreButton?.setOnClickListener(this)
        }
    }
}