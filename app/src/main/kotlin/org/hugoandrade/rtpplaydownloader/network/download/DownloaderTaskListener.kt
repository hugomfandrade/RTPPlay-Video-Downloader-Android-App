package org.hugoandrade.rtpplaydownloader.network.download

import java.io.File

interface DownloaderTaskListener {

    fun downloadStarted(f: File)
    fun onProgress(progress: Float)
    fun onProgress(progress: Long, size: Long)
    fun onProgress(downloadingSpeed: Float, remainingTime: Long)
    fun downloadFinished(f: File)
    fun downloadFailed(message: String?)
}