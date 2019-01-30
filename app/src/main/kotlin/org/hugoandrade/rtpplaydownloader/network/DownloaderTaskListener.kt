package org.hugoandrade.rtpplaydownloader.network

interface DownloaderTaskListener {

    abstract fun onProgress(progress: Float)
}