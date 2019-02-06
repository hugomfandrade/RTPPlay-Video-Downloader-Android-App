package org.hugoandrade.rtpplaydownloader.network

import java.io.File

interface DownloaderTaskListener {

    fun onProgress(progress: Float)
    fun downloadFinished(f: File)
}