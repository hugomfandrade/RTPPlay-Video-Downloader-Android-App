package org.hugoandrade.rtpplaydownloader.network.download

import android.os.Build
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import java.io.*
import java.net.URL
import java.util.stream.Collectors

class TSDownloaderTask(private var playlistUrl : String,
                       private val dirPath : String,
                       private val filename : String,
                       private val listener : Listener,
                       private val tsUrlsValidator: TSUtils.Validator<String>? = null) :

        DownloaderTask(listener) {

    override val TAG : String = javaClass.simpleName

    override fun downloadMediaFile() {

        if (isDownloading) return

        isDownloading = true
        doCanceling = false

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
                isDownloading = false
                dispatchDownloadFailed("file with same name already exists")
                return
            }
            dispatchDownloadStarted(f)

            var progress = 0L
            var size = 0L
            val fos = FileOutputStream(f)
            for ((i, tsUrl) in tsFullUrls.withIndex()) {

                val file = TSUtils.readBulk(tsUrl)
                val inputStream = file?.byteInputStream() ?: continue
                val tsSize = file.encodeToByteArray().size

                size += tsSize
                val estimatedSize = size + tsSize * (tsUrls.size - i - 1)

                val buffer = ByteArray(1024)
                var len = inputStream.read(buffer)
                progress += len.toLong()
                while (len > 0) {

                    if (tryToCancelIfNeeded(fos, inputStream, f)) {
                        // do cancelling
                        return
                    }

                    while (!isDownloading){
                        // pause

                        if (tryToCancelIfNeeded(fos, inputStream, f)) {
                            // do cancelling while paused
                            return
                        }
                    }

                    fos.write(buffer, 0, len)
                    len = inputStream.read(buffer)
                    progress += len

                    if (tryToCancelIfNeeded(fos, inputStream, f)) {
                        // do cancelling while paused
                        return
                    }

                    dispatchProgress(progress, estimatedSize)
                }
                inputStream.close()
            }
            /*
            for ((i, tsUrl) in tsFullUrls.withIndex()) {

                val u = URL(tsUrl)
                val inputStream = u.openStream() ?: continue
                // update size
                val huc = u.openConnection()
                val tsSize = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    huc.contentLengthLong
                } else {
                    huc.contentLength.toLong()
                }

                size += tsSize
                val estimatedSize = size + tsSize * (tsUrls.size - i - 1)

                val buffer = ByteArray(1024)
                var len = inputStream.read(buffer)
                progress += len.toLong()
                while (len > 0) {

                    if (tryToCancelIfNeeded(fos, inputStream, f)) {
                        // do cancelling
                        return
                    }

                    while (!isDownloading){
                        // pause

                        if (tryToCancelIfNeeded(fos, inputStream, f)) {
                            // do cancelling while paused
                            return
                        }
                    }

                    fos.write(buffer, 0, len)
                    len = inputStream.read(buffer)
                    progress += len

                    if (tryToCancelIfNeeded(fos, inputStream, f)) {
                        // do cancelling while paused
                        return
                    }

                    dispatchProgress(progress, estimatedSize)
                }
                inputStream.close()
            }
            */
            dispatchDownloadFinished(f)

            fos.close()

        } catch (ioe: Exception) {
            ioe.printStackTrace()
            dispatchDownloadFailed("Internal error while downloading")
        }
        isDownloading = false
    }
}