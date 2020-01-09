package org.hugoandrade.rtpplaydownloader

import android.app.Activity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import java.util.*

class DrawerItemListAdapter (private val activity: Activity) : RecyclerView.Adapter<DrawerItemListAdapter.ViewHolder>() {

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
            if (isHeader) {
                holder.tvHeader.text = (item as Header).headerTitle
            } else {
                val drawerItem = (item as QuickAccessItem)
                holder.tvTitle.text = drawerItem.title
                holder.ivIcon.setImageResource(drawerItem.resourceID)
            }
        }
    }

    fun setOnItemClickListener(listener: OnDrawerClickListener?) {
        mListener = listener
    }

    fun addItem(item: QuickAccessItem) {
        synchronized(mItemList) { mItemList.add(item) }
    }

    fun addHeader(header: String?) {
        synchronized(mItemList) { mItemList.add(Header(header)) }
    }

    fun getItemAt(position: Int): QuickAccessItem? {
        synchronized(mItemList) {
            var i = 0
            for (item in mItemList) {
                if (item is QuickAccessItem) {
                    if (i == position) {
                        return item
                    } else {
                        i++
                    }
                }
            }
        }
        return null
    }

    interface OnDrawerClickListener {
        fun onItemClicked(drawerItem: QuickAccessItem?)
    }

    abstract class Item

    class Header internal constructor(val headerTitle: String?) : Item()

    data class QuickAccessItem(val resourceID: Int, val title: String, val url: String) : Item()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {

        var drawerHeader: View = view.findViewById(R.id.drawer_header)
        var tvHeader: TextView = view.findViewById(R.id.tv_drawer_header)
        var drawerLayout: View = view.findViewById(R.id.drawer_layout)
        var ivIcon: ImageView = view.findViewById(R.id.iv_drawer_icon)
        var tvTitle: TextView = view.findViewById(R.id.tv_drawer_title)

        override fun onClick(v: View) {
            val drawerItem = getItemAt(adjustPosition(adapterPosition))
            if (drawerItem != null) {
                mListener?.onItemClicked(getItemAt(adjustPosition(adapterPosition)))
            }
        }

        init {
            drawerLayout.setOnClickListener(this)
            ivIcon = view.findViewById(R.id.iv_drawer_icon)
            tvTitle = view.findViewById(R.id.tv_drawer_title)
        }
    }

    private fun adjustPosition(adapterPosition: Int): Int {
        synchronized(mItemList) {
            var i = 0
            var adapterI = 0
            for (item in mItemList) {
                if (item is QuickAccessItem) {
                    if (i == adapterPosition) {
                        return adapterI
                    } else {
                        adapterI++
                    }
                }
                i++
            }
        }
        return adapterPosition
    }

    companion object {
        private val TAG = DrawerItemListAdapter::class.java.simpleName
    }

}