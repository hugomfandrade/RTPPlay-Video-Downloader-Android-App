package org.hugoandrade.rtpplaydownloader.dev.download

import org.hugoandrade.rtpplaydownloader.dev.DevConstants
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTask
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.concurrent.withLock

class DevDownloaderTask(listener : Listener) : DownloaderTask(listener) {

    override fun downloadMediaFile() {

        if (isDownloading) return

        doCanceling = false
        this.resume()

        try {

            val f = File("dev.com")

            Thread.sleep(1000)

            dispatchDownloadStarted(f)

            val downloadSize = 1020
            val length = TimeUnit.SECONDS.toSeconds(20)
            val sizeLimit : Long = length * downloadSize

            var progress : Long = 0

            while (progress < sizeLimit) {

                if (tryToCancelIfNeeded()) {
                    // do cancelling
                    return
                }

                while (!isDownloading){
                    if (DevConstants.showLog) println("paused")
                    lock.withLock {}

                    if (tryToCancelIfNeeded()) {
                        // do cancelling while paused
                        return
                    }
                }

                Thread.sleep(1000)

                progress += downloadSize

                if (tryToCancelIfNeeded()) {
                    // do cancelling while paused
                    return
                }

                dispatchProgress(progress, sizeLimit)
            }

            dispatchDownloadFinished(f)

        } catch (ioe: Exception) {
            ioe.printStackTrace()
            dispatchDownloadFailed("Internal error while downloading")
        } finally {
            isDownloading = false
        }
    }


    private fun tryToCancelIfNeeded(): Boolean {

        if (doCanceling) {
            Thread.sleep(1000)

            dispatchDownloadFailed("cancelled")

            isDownloading = false
            doCanceling = false
            return true
        }
        return false
    }
}