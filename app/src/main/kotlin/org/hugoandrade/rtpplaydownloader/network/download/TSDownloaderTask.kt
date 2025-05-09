package org.hugoandrade.rtpplaydownloader.network.download

import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.stream.Collectors
import kotlin.math.max

class TSDownloaderTask(private var playlistUrl : String,
                       private val dirPath : String,
                       private val filename : String,
                       private val listener : Listener,
                       private val tsUrlsValidator: TSUtils.Validator<String>? = null) :

        DownloaderTask(listener) {

    override fun downloadMediaFile() {

        // check if was cancelled before actually starting
        if (tryToCancelIfNeeded()) return

        try {
            try {
                URL(playlistUrl)
            }
            catch (e: Exception) {
                dispatchDownloadFailed("URL no longer exists")
                return
            }

            val m3u8: String = playlistUrl
            val baseUrl: String = m3u8.substring(0, m3u8.lastIndexOf("/") + 1)

            val tsUrls = if(tsUrlsValidator == null) {
                TSUtils.getTSUrls(playlistUrl)
            }
            else {
                TSUtils.getTSUrls(playlistUrl, tsUrlsValidator)
            }

            // full urls
            val tsFullUrls = tsUrls
                    .stream()
                    .map { tsUrl -> baseUrl + tsUrl }
                    .collect(Collectors.toList())

            val storagePath = dirPath
            val f = File(storagePath, filename)
            if (MediaUtils.doesMediaFileExist(f)) {
                dispatchDownloadFailed("file with same name already exists")
                return
            }

            dispatchDownloadStarted(f)

            var progress = 0L
            var size = 0L
            val fos = FileOutputStream(f)
            for ((i, tsUrl) in tsFullUrls.withIndex()) {

                val inputStream = TSUtils.readBulkAsInputStream(tsUrl) ?: continue

                // System.err.println(tsSize)
                // System.err.println("===")

                /*
                val u = URL(tsUrl)
                val inputStream = u.openStream() ?: continue
                // update size
                val huc = u.openConnection()
                val tsSize = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    huc.contentLengthLong
                } else {
                    huc.contentLength.toLong()
                }
                */

                val buffer = ByteArray(1024)
                var len = inputStream.read(buffer)
                progress += len.toLong()
                while (len > 0) {

                    // cancel before downloading
                    if (tryToCancelIfNeeded()) return

                    if (doPause()) return

                    fos.write(buffer, 0, len)
                    len = inputStream.read(buffer)
                    progress += len

                    // cancel after downloading
                    if (tryToCancelIfNeeded()) return

                    val tsSize = progress / (max(1, i))
                    val estimatedSize = progress + tsSize * (tsUrls.size - i - 1)

                    dispatchProgress(progress, estimatedSize)
                }
                inputStream.close()
            }

            dispatchDownloadFinished(f)

            fos.close()

        } catch (ioe: Exception) {
            ioe.printStackTrace()
            dispatchDownloadFailed("Internal error while downloading")
        }
    }
}