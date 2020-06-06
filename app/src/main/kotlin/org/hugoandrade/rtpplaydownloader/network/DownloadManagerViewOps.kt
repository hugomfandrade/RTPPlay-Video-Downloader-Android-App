package org.hugoandrade.rtpplaydownloader.network

interface DownloadManagerViewOps {
    fun displayDownloadableItems(actions: List<DownloadableItemAction>)
    fun displayDownloadableItem(action: DownloadableItemAction)
}