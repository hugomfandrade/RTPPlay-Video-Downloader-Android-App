package org.hugoandrade.rtpplaydownloader.network.download

import android.os.Build
import org.hugoandrade.rtpplaydownloader.network.parsing.TSParsingTask
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlin.collections.ArrayList

class TSDownloaderTask(private val url : String?,
                       private var playlistUrl : String,
                       private val dirPath : String,
                       private val filename : String,
                       private val listener : Listener,
                       private val tsUrlsValidator: TSUtils.Validator<String>? = null,
                       private val reParsingTask : TSParsingTask? = null) :

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

            /*
            if (url != null && reParsingTask != null) {
                // try reparse
                Log.d(TAG, "try reparse $url")
                val parsingTask = reParsingTask
                parsingTask.parseMediaFile(url)
                val parsingMediaUrl = parsingTask.mediaUrl
                if (parsingMediaUrl != null) {
                    playlistUrl = TSUtils.getM3U8Playlist(parsingMediaUrl)
                }
            }
            val playlistUrl = baseUrl + playlist*/
            val tsUrls = if(tsUrlsValidator == null) {
                TSUtils.getTSUrls(playlistUrl)
            }
            else {
                TSUtils.getTSUrls(playlistUrl, tsUrlsValidator)
            }

            // full urls
            /*
            val tsFullUrls: ArrayList<String> = tsUrls
                    .stream()
                    .map { tsUrl -> baseUrl + tsUrl }
                    .collect(Collectors.toList())
            */
            val tsFullUrls: ArrayList<String> = ArrayList()
            for (tsUrl in tsUrls) {
                val tsFullUrl = baseUrl + tsUrl
                tsFullUrls.add(tsFullUrl)
            }

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

                val u = URL(tsUrl)
                val inputStream = u.openStream()
                if (inputStream != null) {
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

            }
            dispatchDownloadFinished(f)

            fos.close()

        } catch (ioe: Exception) {
            ioe.printStackTrace()
            dispatchDownloadFailed("Internal error while downloading")
        }
        isDownloading = false
    }
}