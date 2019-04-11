package org.hugoandrade.rtpplaydownloader.network

interface DownloadableItemStateChangeSupport {
    fun addDownloadStateChangeListener(downloadableItemStateChangeListener: DownloadableItemStateChangeListener)
    fun removeDownloadStateChangeListener(downloadableItemStateChangeListener: DownloadableItemStateChangeListener)
}