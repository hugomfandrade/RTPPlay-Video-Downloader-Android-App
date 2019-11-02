package org.hugoandrade.rtpplaydownloader.network

import org.hugoandrade.rtpplaydownloader.common.ContextView

interface DownloadManagerViewOps : ContextView {
    fun displayDownloadableItems(actions: List<DownloadableItemAction>)
    fun displayDownloadableItem(action: DownloadableItemAction)
}