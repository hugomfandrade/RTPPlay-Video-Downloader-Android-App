package org.hugoandrade.rtpplaydownloader.network

interface DownloadableItemStateChangeListener {
    fun onDownloadStateChange(downloadableItem: DownloadableItem)
}