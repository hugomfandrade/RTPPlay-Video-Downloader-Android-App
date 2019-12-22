package org.hugoandrade.rtpplaydownloader.network

enum class DownloadableItemState {
    Start,
    Downloading,
    End,

    Failed;

    interface ChangeListener {
        fun onDownloadStateChange(downloadableItem: DownloadableItem)
    }
}