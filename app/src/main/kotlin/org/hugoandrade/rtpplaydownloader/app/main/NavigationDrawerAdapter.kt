package org.hugoandrade.rtpplaydownloader.app.main

import android.app.Activity
import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.hugoandrade.rtpplaydownloader.R
import java.util.*

class NavigationDrawerAdapter (private val activity: Activity) : RecyclerView.Adapter<NavigationDrawerAdapter.ViewHolder>() {

    companion object {
        private val TAG = NavigationDrawerAdapter::class.java.simpleName
    }

    private val mItemList: MutableList<Item> = ArrayList()
    private var mListener: OnDrawerClickListener? = null

    override fun getItemCount(): Int {
        return mItemList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val vi = LayoutInflater.from(parent.context)
        return ViewHolder(vi.inflate(R.layout.list_item_drawer, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        synchronized(mItemList) {
            val item = mItemList[holder.adapterPosition]
            val isHeader = item is Header
            holder.drawerHeader.visibility = if (isHeader) View.VISIBLE else View.GONE
            holder.drawerLayout.visibility = if (isHeader) View.GONE else View.VISIBLE
            when (item) {
                is Header -> {
                    holder.tvHeader.text = item.headerTitle
                }
                is QuickAccessItem -> {
                    holder.tvTitle.text = item.title
                    holder.ivIcon.setImageResource(item.resourceID)
                }
                is OptionItem -> {
                    holder.tvTitle.text = item.title
                    holder.ivIcon.setImageResource(item.resourceID)
                }
                else -> {
                    holder.drawerLayout.visibility = View.GONE
                }
            }
        }
    }

    fun setOnItemClickListener(listener: OnDrawerClickListener?) {
        mListener = listener
    }

    fun addOptionItem(item: OptionItem) {
        synchronized(mItemList) { mItemList.add(item) }
    }

    fun addItem(item: QuickAccessItem) {
        synchronized(mItemList) { mItemList.add(item) }
    }

    fun addHeader(header: String?) {
        synchronized(mItemList) { mItemList.add(Header(header)) }
    }

    fun getItemAt(position: Int): Item? {
        synchronized(mItemList) {
            return mItemList[position]
        }
    }

    interface OnDrawerClickListener {
        fun onItemClicked(drawerItem: Item?)
    }

    abstract class Item

    class Header internal constructor(val headerTitle: String?) : Item()

    data class OptionItem(val resourceID: Int, val title: String, val intent: Intent) : Item()

    data class QuickAccessItem(val resourceID: Int, val title: String, val url: String) : Item()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {

        var drawerHeader: View = view.findViewById(R.id.drawer_header)
        var tvHeader: TextView = view.findViewById(R.id.tv_drawer_header)
        var drawerLayout: View = view.findViewById(R.id.drawer_layout)
        var ivIcon: ImageView = view.findViewById(R.id.iv_drawer_icon)
        var tvTitle: TextView = view.findViewById(R.id.tv_drawer_title)

        override fun onClick(v: View) {
            val drawerItem = getItemAt(adapterPosition)
            if (drawerItem != null) {
                mListener?.onItemClicked(drawerItem)
            }
        }

        init {
            drawerLayout.setOnClickListener(this)
            ivIcon = view.findViewById(R.id.iv_drawer_icon)
            tvTitle = view.findViewById(R.id.tv_drawer_title)
        }
    }
}