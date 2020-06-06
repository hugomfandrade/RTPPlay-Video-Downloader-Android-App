package org.hugoandrade.rtpplaydownloader.widget

import android.content.Context
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.hugoandrade.rtpplaydownloader.R

class NavigationDrawerLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0)
    : androidx.drawerlayout.widget.DrawerLayout(context, attrs, defStyle) {

    val coordinatorLayout: androidx.coordinatorlayout.widget.CoordinatorLayout?
    val navigationDrawerContent: androidx.recyclerview.widget.RecyclerView

    init {
        val inflater = getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.navigation_drawer_layout, this, true)

        coordinatorLayout = findViewById(R.id.coordinator_layout)
        navigationDrawerContent = findViewById(R.id.drawer_content)
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        if (coordinatorLayout != null) {
            coordinatorLayout.addView(child, index, params)
        } else {
            super.addView(child, index, params)
        }
    }

    override fun addViewInLayout(child: View, index: Int, params: ViewGroup.LayoutParams, preventRequestLayout: Boolean): Boolean {
        return false
    }
}