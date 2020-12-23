package org.hugoandrade.rtpplaydownloader.network.download

import android.os.Build
import android.util.Log
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.TVIPlayerParsingTask
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.collections.ArrayList

open class TVIPlayerTSDownloaderTask(private val url : String?,
                                     private val mediaUrl : String,
                                     private val dirPath : String,
                                     private val filename : String,
                                     private val listener : Listener) :

        DownloaderTask(listener) {

    override val TAG : String = javaClass.simpleName

    private val tsUrlsValidator = object : TSUtils.Validator<String> {
        override fun isValid(o: String): Boolean {
            return o.startsWith("media")
        }
    }

    override fun downloadMediaFile() {

        if (isDownloading) return

        isDownloading = true
        doCanceling = false

        try {
            try {
                URL(mediaUrl)
            }
            catch (e: Exception) {
                dispatchDownloadFailed("URL no longer exists")
                return
            }

            val m3u8: String = mediaUrl
            val baseUrl: String = m3u8.substring(0, m3u8.lastIndexOf("/") + 1)
            var playlist = TSUtils.getM3U8Playlist(m3u8)

            if (playlist == null && url != null) {
                // try reparse
                Log.d(TAG, "try reparse $url")
                val parsingTask = TVIPlayerParsingTask()
                parsingTask.parseMediaFile(url)
                val parsingMediaUrl = parsingTask.mediaUrl
                if (parsingMediaUrl != null) {
                    playlist = TSUtils.getM3U8Playlist(parsingMediaUrl)
                }
            }
            val playlistUrl = baseUrl + playlist
            val tsUrls = TSUtils.getTSUrls(playlistUrl, tsUrlsValidator)
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
                    val huc = u.openConnection() as HttpURLConnection //to know the size of video
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