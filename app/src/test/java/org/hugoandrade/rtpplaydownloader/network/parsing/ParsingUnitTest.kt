package org.hugoandrade.rtpplaydownloader.network.parsing

import com.google.common.util.concurrent.AtomicDouble
import org.hugoandrade.rtpplaydownloader.network.DownloadableItem
import org.hugoandrade.rtpplaydownloader.network.download.*
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.ParsingIdentifier
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import java.io.File
import kotlin.math.roundToInt

open class ParsingUnitTest {

    var DO_DOWNLOAD = true
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

    internal fun debug(parsingData: ParsingData?) {
        System.err.println("successfully parsed ? " + (parsingData != null))
        System.err.println(parsingData)
        System.err.println(parsingData?.m3u8Playlist?.getTSUrls()?.firstOrNull()?.url)
    }

    internal fun download(item: DownloadableItem?) {
        if (item == null) return
        if (!DO_DOWNLOAD) return

        // clone with unique filename
        val downloadableItem = DownloadableItem(
                url = item.url,
                mediaUrl = item.mediaUrl,
                filename = MediaUtils.getUniqueFilenameAndLock(testDir.absolutePath, item.filename ?: ""),
                thumbnailUrl = item.thumbnailUrl,
                downloadTask = item.downloadTask
        )

        println(item.downloadTask)
        println(downloadableItem.downloadTask)

        val downloaderTask = DownloaderIdentifier.findTask(testDir.absolutePath, downloadableItem, defaultListener)

        System.err.println("about to download: ${downloaderTask.javaClass.simpleName}")

        downloaderTask.downloadMediaFile()
    }

    internal fun download(parsingData: ParsingData?) {
        if (parsingData == null) return

        val tsUrl = parsingData.m3u8Playlist?.getTSUrls()?.firstOrNull()

        val item : DownloadableItem = if (tsUrl == null) {
            DownloadableItem(parsingData)
        }
        else {
            DownloadableItem(
                    url = parsingData.url ?: null.toString(),
                    mediaUrl = tsUrl.url,
                    thumbnailUrl = parsingData.thumbnailUrl ?: null.toString(),
                    filename = parsingData.filename ?: null.toString()
            )
        }

        item.downloadTask = ParsingIdentifier.findType(ParsingIdentifier.findHost(parsingData.url))?.name

        download(item)
    }
}