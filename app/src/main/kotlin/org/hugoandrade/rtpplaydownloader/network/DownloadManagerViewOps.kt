package org.hugoandrade.rtpplaydownloader.network

interface DownloadManagerViewOps {
    fun onParsingError(url: String, message : String)
    fun onParsingSuccessful(item: DownloadableItem)
}