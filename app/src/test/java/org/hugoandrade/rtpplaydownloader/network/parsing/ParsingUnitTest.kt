package org.hugoandrade.rtpplaydownloader.network.parsing

import com.google.common.util.concurrent.AtomicDouble
import org.hugoandrade.rtpplaydownloader.network.DownloadableItem
import org.hugoandrade.rtpplaydownloader.network.download.*
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.ParsingTask
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



    internal fun debug(parsingTask: TSParsingTask) {

        val mediaUrl : String? = parsingTask.mediaUrl
        val playlist : TSPlaylist? = parsingTask.getTSPlaylist()
        val playlistUrl : String? = playlist?.getTSUrls()?.firstOrNull()?.url

        System.err.println(playlist?.getTSUrls())
        System.err.println(mediaUrl)
        System.err.println(playlistUrl)
    }

    internal fun debug(parsingTask: ParsingTask) {

        val mediaUrl : String? = parsingTask.mediaUrl

        System.err.println(mediaUrl)
    }

    internal fun download(item: DownloadableItem) {


        val downloaderTask = DownloaderIdentifier.findTask(testDir.absolutePath, item, defaultListener)

        System.err.println("about to download: ${downloaderTask.javaClass.simpleName}")

        downloaderTask.downloadMediaFile()
    }

    internal fun download(parsingTask: TSParsingTask) {

        val mediaUrl : String? = parsingTask.mediaUrl
        val playlist : TSPlaylist? = parsingTask.getTSPlaylist()
        val playlistUrl : String = playlist?.getTSUrls()?.firstOrNull()?.url ?: ""
        val mediaFilename : String = MediaUtils.getUniqueFilenameAndLock(testDir.absolutePath, parsingTask.filename ?: "")

        val downloaderTask = TSDownloaderTask(
                playlistUrl,
                testDir.absolutePath,
                mediaFilename,
                defaultListener,
                object : TSUtils.Validator<String> {
                    override fun isValid(o: String): Boolean {
                        return o.contains(".ts")
                    }
                }
        )

        downloaderTask.downloadMediaFile()
    }

    internal fun download(parsingTask: ParsingTask) {

        val mediaUrl : String = parsingTask.mediaUrl ?: ""
        val mediaFilename : String = MediaUtils.getUniqueFilenameAndLock(testDir.absolutePath, parsingTask.filename ?: "")

        val downloaderTask = RawDownloaderTask(
                mediaUrl,
                testDir.absolutePath,
                mediaFilename,
                defaultListener)

        downloaderTask.downloadMediaFile()
    }
}