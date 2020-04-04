package org.hugoandrade.rtpplaydownloader.network

interface DownloadableItemActionAPI {

    fun startDownload()

    fun cancel()

    fun resume()

    fun pause()

    fun refresh()

    fun play()
}