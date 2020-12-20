package org.hugoandrade.rtpplaydownloader.network.parsing

import com.google.common.util.concurrent.AtomicDouble
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTask
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import java.io.File
import kotlin.math.roundToInt

open class ParsingUnitTest {

    val testDir = File("test-download-folder")
    val defaultListener: DownloaderTask.Listener = object : DownloaderTask.Listener {

        private val progressLogPercentageDelta = 1.0
        private val progressLogLastPercentage = AtomicDouble(Double.NaN)

        override fun downloadStarted(f: File) {
            System.err.println("downloadStarted " + f)
            progressLogLastPercentage.set(Double.NaN)
        }

        override fun onProgress(downloadedSize: Long, totalSize: Long) {

            val progress = downloadedSize.toDouble() / totalSize.toDouble() * 100
            val readableDownloadSize = MediaUtils.humanReadableByteCount(downloadedSize, true)
            val readableTotalSize = MediaUtils.humanReadableByteCount(totalSize, true)

            if (!progressLogPercentageDelta.isNaN()) {
                when {
                    progressLogLastPercentage.get().isNaN() -> {
                        progressLogLastPercentage.set(progress + progressLogPercentageDelta)
                    }
                    progressLogLastPercentage.get() < progress -> {
                        progressLogLastPercentage.set(progress + progressLogPercentageDelta)
                    }
                    else -> {
                        return
                    }
                }
            }

            // System.err.println("onProgress " + downloadedSize + " - " + totalSize)
            System.err.println("onProgress " + progress.roundToInt() + "%" + " (" + readableTotalSize + ")")
        }

        override fun downloadFinished(f: File) {
            System.err.println("downloadFinished " + f)
        }

        override fun downloadFailed(message: String?) {
            System.err.println("downloadFailed " + message)
        }
    }


}