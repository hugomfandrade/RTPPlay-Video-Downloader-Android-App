package org.hugoandrade.rtpplaydownloader.network.parsing

import android.databinding.DataBindingUtil
import android.databinding.ObservableBoolean
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import org.hugoandrade.rtpplaydownloader.R
import org.hugoandrade.rtpplaydownloader.databinding.ParsingItemBinding
import org.hugoandrade.rtpplaydownloader.databinding.ParsingItemLoadingBinding
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.ParsingTaskBase
import java.io.File
import kotlin.collections.ArrayList

class ParsingItemsAdapter : RecyclerView.Adapter<ParsingItemsAdapter.ViewHolder>() {

    private var showLoadMore: Boolean = false
    private var showProgressBar: Boolean = false
    private val recyclerViewLock: Any = Object()
    private var recyclerView: RecyclerView? = null
    private var listener: Listener? = null

    private val parsingItemList: ArrayList<ParsingItem> = ArrayList()

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
        return if (position == parsingItemList.size) 1 else 0
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        if (holder.binding != null) {
            val parsingItem: ParsingItem = parsingItemList[position]
            val parsingItemCount: Int = parsingItemList.size
            holder.binding.item = parsingItem

            // THUMBNAIL

            val dir : File? = recyclerView?.context?.getExternalFilesDir(null)
            val thumbnailUrl : String? = parsingItem.task.thumbnailUrl

            holder.binding.parsingItemMediaImageView.setImageResource(R.drawable.media_file_icon)
            org.hugoandrade.rtpplaydownloader.utils.ImageHolder.displayImage(dir, thumbnailUrl, holder.binding.parsingItemMediaImageView)

            holder.binding.selectedCheckBox.visibility = if (parsingItemCount > 1) View.VISIBLE else View.GONE
        }

        if (holder.bindingLoading != null) {

            holder.bindingLoading.loadMoreButton.visibility = if (showLoadMore)
                View.VISIBLE else
                View.GONE
            holder.bindingLoading.progressBarView.visibility = if (showProgressBar)
                View.VISIBLE else
                View.GONE
        }

    }

    override fun getItemCount(): Int {
        return parsingItemList.size + if (showLoadMore || showProgressBar) 1 else 0
    }

    fun add(task: ParsingTaskBase) {
        synchronized(parsingItemList) {
            for (parsingItem in parsingItemList) {
                if (parsingItem.task == task) {
                    return
                }
            }
            parsingItemList.add(ParsingItem(task, ObservableBoolean(true)))
        }
    }

    fun addAndNotify(task: ParsingTaskBase) {
        synchronized(parsingItemList) {
            for (parsingItem in parsingItemList) {
                if (parsingItem.task == task) {
                    return
                }
            }
            parsingItemList.add(ParsingItem(task, ObservableBoolean(true)))
            notifyItemInserted(parsingItemList.size - 1)
            notifyItemRangeChanged(parsingItemList.size - 1, parsingItemList.size)
        }
    }

    fun remove(task: ParsingTaskBase) {
        synchronized(parsingItemList) {
            for ((index, parsingItem) in parsingItemList.withIndex()) {
                if (parsingItem.task == task) {
                    parsingItemList.removeAt(index)
                    notifyItemRemoved(index)
                    notifyItemRangeChanged(index, parsingItemList.size)
                    return
                }
            }
        }
    }

    fun clear() {
        synchronized(parsingItemList) {
            parsingItemList.clear()
        }
    }

    fun addAllAndNotify(tasks: ArrayList<ParsingTaskBase>) {
        synchronized(parsingItemList) {
            val prevItemCount: Int = itemCount
            tasks.forEach(action = { task ->
                var alreadyInList = false
                for (parsingItem in parsingItemList) {
                    if (parsingItem.task == task) {
                        alreadyInList = true
                        break
                    }
                }
                if (!alreadyInList) {
                    parsingItemList.add(ParsingItem(task, ObservableBoolean(true)))
                }
            })
            if (prevItemCount != itemCount) {
                notifyItemRangeChanged(prevItemCount, parsingItemList.size)
            }
        }
    }

    fun addAll(tasks: ArrayList<ParsingTaskBase>) {
        synchronized(parsingItemList) {
            tasks.forEach(action = { task ->
                var alreadyInList = false
                for (parsingItem in parsingItemList) {
                    if (parsingItem.task == task) {
                        alreadyInList = true
                        break
                    }
                }
                if (!alreadyInList) {
                    parsingItemList.add(ParsingItem(task, ObservableBoolean(true)))
                }
            })
        }
    }

    fun getSelectedTasks(): ArrayList<ParsingTaskBase> {
        synchronized(parsingItemList) {
            val tasks : ArrayList<ParsingTaskBase> = ArrayList()
            for (parsingItem in parsingItemList) {
                if (parsingItem.isSelected.get()) {
                    tasks.add(parsingItem.task)
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

            RecyclerView.ViewHolder(binding?.root?: bindingLoading?.root ?: View(null)),
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
            synchronized(parsingItemList) {
                item = parsingItemList[adapterPosition]
                item.isSelected.set(isChecked)
            }
        }

        init {
            binding?.selectedCheckBox?.setOnCheckedChangeListener(this)
            bindingLoading?.loadMoreButton?.setOnClickListener(this)
        }
    }
}