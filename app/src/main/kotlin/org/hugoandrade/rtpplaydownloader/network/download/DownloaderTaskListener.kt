package org.hugoandrade.rtpplaydownloader.network.download

import java.io.File

interface DownloaderTaskListener {

    fun downloadStarted(f: File)
    fun onProgress(progress: Float)
    fun downloadFinished(f: File)
    fun downloadFailed(message: String?)
}