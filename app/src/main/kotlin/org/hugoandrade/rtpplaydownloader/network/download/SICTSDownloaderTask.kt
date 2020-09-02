package org.hugoandrade.rtpplaydownloader.network.download

import android.os.Build
import android.util.Log
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.SICParsingTaskV2
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

open class SICTSDownloaderTask(private val url : String?,
                               private val mediaUrl : String,
                               private val dirPath : String,
                               private val filename : String,
                               private val listener : Listener) : DownloaderTask(mediaUrl, dirPath, filename, listener) {

    override val TAG : String = javaClass.simpleName

    private val m3u8Validator = object : TSUtils.Validator<String>{
        override fun isValid(o: String): Boolean {
            return o.contains(".m3u8")
        }
    }

    override fun downloadMediaFile() {

        if (isDownloading) {
            return
        }

        isDownloading = true
        doCanceling = false

        try {
            try {
                URL(mediaUrl)
            }
            catch (e: Exception) {
                listener.downloadFailed("URL no longer exists")
                return
            }

            val m3u8: String = mediaUrl
            val baseUrl: String = m3u8.substring(0, m3u8.lastIndexOf("/") + 1)
            var playlist = TSUtils.getM3U8Playlist(m3u8, m3u8Validator)

            if (playlist == null && url != null) {
                // try reparse
                Log.d(TAG, "try reparse $url")
                val parsingTask = SICParsingTaskV2()
                parsingTask.parseMediaFile(url)
                val parsingMediaUrl = parsingTask.mediaUrl
                if (parsingMediaUrl != null) {
                    playlist = TSUtils.getM3U8Playlist(parsingMediaUrl, m3u8Validator)
                }
            }

            val playlistUrl = baseUrl + playlist
            val tsUrls = TSUtils.getTSUrls(playlistUrl)

            if (tsUrls.isEmpty()) {
                listener.downloadFailed("failed to get ts files")
                return
            }

            val tsFullUrls: ArrayList<String> = ArrayList()
            for (tsUrl in tsUrls) {
                val tsFullUrl = baseUrl + tsUrl
                tsFullUrls.add(tsFullUrl)
            }

            val storagePath = dirPath
            val f = File(storagePath, filename)
            if (MediaUtils.doesMediaFileExist(f)) {
                isDownloading = false
                listener.downloadFailed("file with same name already exists")
                return
            }
            listener.downloadStarted(f)

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

                        listener.onProgress(progress, estimatedSize)
                    }
                    inputStream.close()
                }

            }
            listener.downloadFinished(f)

            fos.close()

        } catch (ioe: Exception) {
            ioe.printStackTrace()
            listener.downloadFailed("Internal error while downloading")
        }
        isDownloading = false
    }

    override fun cancel() {
        doCanceling = true
    }

    override fun resume() {
        isDownloading = true
    }

    override fun pause() {
        isDownloading = false
    }

    private fun tryToCancelIfNeeded(fos: FileOutputStream, inputStream: InputStream, f: File): Boolean {

        if (doCanceling) {
            fos.close()
            try {
                inputStream.close()
            } catch (ioe: IOException) {
                // just going to ignore this one
            }
            f.delete()

            listener.downloadFailed("cancelled")
            isDownloading = false
            doCanceling = false
            return true
        }
        return false
    }
}