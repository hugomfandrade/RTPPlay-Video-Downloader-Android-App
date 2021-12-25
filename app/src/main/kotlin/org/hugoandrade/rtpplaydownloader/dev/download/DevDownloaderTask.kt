package org.hugoandrade.rtpplaydownloader.dev.download

import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTask
import java.io.File
import java.util.concurrent.TimeUnit

class DevDownloaderTask(listener : Listener) : DownloaderTask(listener) {

    override fun downloadMediaFile() {

        // check if was cancelled before actually starting
        if (tryToCancelIfNeeded()) return

        try {

            val f = File("dev.com")

            Thread.sleep(1000)

            dispatchDownloadStarted(f)

            val downloadSize = 1020
            val length = TimeUnit.SECONDS.toSeconds(20)
            val sizeLimit : Long = length * downloadSize

            var progress : Long = 0

            while (progress < sizeLimit) {

                // cancel before downloading
                if (tryToCancelIfNeeded()) return

                if (doPause()) return

                // actual download
                Thread.sleep(1000)
                progress += downloadSize

                // cancel after downloading
                if (tryToCancelIfNeeded()) return

                dispatchProgress(progress, sizeLimit)
            }

            dispatchDownloadFinished(f)

        } catch (ioe: Exception) {
            ioe.printStackTrace()
            dispatchDownloadFailed("Internal error while downloading")
        }
    }
}