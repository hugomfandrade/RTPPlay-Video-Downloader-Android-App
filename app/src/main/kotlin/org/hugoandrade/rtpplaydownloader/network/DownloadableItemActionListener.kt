package org.hugoandrade.rtpplaydownloader.network

interface DownloadableItemActionListener {
    fun onPlay(action : DownloadableItemAction)
    fun onRefresh(action: DownloadableItemAction)
}