package org.hugoandrade.rtpplaydownloader.network.parsing

import com.google.common.util.concurrent.AtomicDouble
import org.hugoandrade.rtpplaydownloader.network.DownloadableItem
import org.hugoandrade.rtpplaydownloader.network.download.*
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.ParsingTask
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.TSParsingTask
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import java.io.File
import kotlin.math.roundToInt

open class ParsingUnitTest {

    val DO_DOWNLOAD = false
    private val testDir = File("test-download-folder")
    private val defaultListener: DownloaderTask.Listener = object : DownloaderTask.Listener {

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



    internal fun debug(parsingTask: TSParsingTask) {

        val playlist : TSPlaylist? = parsingTask.getTSPlaylist()
        val playlistUrl : String? = playlist?.getTSUrls()?.firstOrNull()?.url

        System.err.println(parsingTask.filename)
        System.err.println(parsingTask.mediaUrl)
        System.err.println(playlistUrl)
        System.err.println(playlist?.getTSUrls())
    }

    internal fun debug(parsingTask: ParsingTask) {

        System.err.println(parsingTask.filename)
        System.err.println(parsingTask.mediaUrl)
    }

    internal fun download(item: DownloadableItem) {
        if (!DO_DOWNLOAD) return

        // clone with unique filename
        val downloadableItem = DownloadableItem(
                url = item.url,
                mediaUrl = item.mediaUrl,
                filename = MediaUtils.getUniqueFilenameAndLock(testDir.absolutePath, item.filename ?: ""),
                thumbnailUrl = item.thumbnailUrl,
                downloadTask = item.downloadTask
        )

        val downloaderTask = DownloaderIdentifier.findTask(testDir.absolutePath, downloadableItem, defaultListener)

        System.err.println("about to download: ${downloaderTask.javaClass.simpleName}")

        downloaderTask.downloadMediaFile()
    }

    internal fun download(parsingTask: TSParsingTask) {
        download(DownloadableItem(parsingTask))
    }

    internal fun download(parsingTask: ParsingTask) {
        download(DownloadableItem(parsingTask))
    }
}