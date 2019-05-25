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
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTaskBase
import kotlin.collections.ArrayList

class ParsingItemsAdapter : RecyclerView.Adapter<ParsingItemsAdapter.ViewHolder>() {

    private val recyclerViewLock: Any = Object()
    private var recyclerView: RecyclerView? = null

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
        val binding : ParsingItemBinding = DataBindingUtil.inflate(layoutInflater, R.layout.parsing_item, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val parsingItem: ParsingItem = parsingItemList[position]
        holder.binding.item = parsingItem

        holder.binding.selectedCheckBox.visibility = if (itemCount > 1) View.VISIBLE else View.GONE

    }

    override fun getItemCount(): Int {
        return parsingItemList.size
    }

    fun add(task: DownloaderTaskBase) {
        synchronized(parsingItemList) {
            for (parsingItem in parsingItemList) {
                if (parsingItem.task == task) {
                    return
                }
            }
            parsingItemList.add(ParsingItem(task, ObservableBoolean(true)))
            notifyItemInserted(parsingItemList.size)
            notifyItemRangeChanged(parsingItemList.size, itemCount)
        }
    }

    fun remove(task: DownloaderTaskBase) {
        synchronized(parsingItemList) {
            var index = 0
            for (parsingItem in parsingItemList) {
                if (parsingItem.task == task) {
                    parsingItemList.removeAt(index)
                    notifyItemRemoved(index)
                    notifyItemRangeChanged(index, itemCount)
                    return
                }
                index++
            }
        }
    }

    fun clear() {
        synchronized(parsingItemList) {
            parsingItemList.clear()
        }
    }

    fun addAll(tasks: ArrayList<DownloaderTaskBase>) {

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

    inner class ViewHolder(val binding: ParsingItemBinding) :
            RecyclerView.ViewHolder(binding.root),
            CompoundButton.OnCheckedChangeListener {

        override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
            val item : ParsingItem
            synchronized(parsingItemList) {
                item = parsingItemList[adapterPosition]
                item.isSelected.set(isChecked)
            }
        }

        init {
            binding.selectedCheckBox.setOnCheckedChangeListener(this)
        }
    }
}