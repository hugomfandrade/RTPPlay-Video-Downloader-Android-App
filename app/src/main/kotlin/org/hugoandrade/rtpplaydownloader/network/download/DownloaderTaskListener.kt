package org.hugoandrade.rtpplaydownloader.network.download

import java.io.File

interface DownloaderTaskListener {

    fun downloadStarted(f: File)
    fun onProgress(progress: Float)
    fun onProgress(progress: Long, size: Long)
    fun downloadFinished(f: File)
    fun downloadFailed(message: String?)
}