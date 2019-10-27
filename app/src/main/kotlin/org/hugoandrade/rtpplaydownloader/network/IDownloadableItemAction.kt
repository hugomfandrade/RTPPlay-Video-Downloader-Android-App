package org.hugoandrade.rtpplaydownloader.network

interface IDownloadableItemAction {

    fun startDownload()

    fun cancel()

    fun resume()

    fun pause()

    fun refresh()

    fun play()
}