package org.hugoandrade.rtpplaydownloader.network.download

import android.os.Build
import android.util.Log
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.RTPPlayV2ParsingTask
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.TVIPlayerParsingTask
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList

open class RTPPlayTSDownloaderTask(private val url : String?,
                                   private val mediaUrl : String,
                                   private val dirPath : String,
                                   private val filename : String,
                                   private val listener : DownloaderTaskListener) : DownloaderTask(mediaUrl, dirPath, filename, listener) {

    override val TAG : String = javaClass.simpleName

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

            var tsUrls = getTSUrls(m3u8)

            if (tsUrls == null && url != null) {
                // try reparse
                Log.d(TAG, "try reparse $url")
                val parsingTask = RTPPlayV2ParsingTask()
                parsingTask.parseMediaFile(url)
                val parsingMediaUrl = parsingTask.mediaUrl
                if (parsingMediaUrl != null) {
                    tsUrls = getTSUrls(parsingMediaUrl)
                }
            }

            val tsFullUrls: ArrayList<String> = ArrayList()
            for (tsUrl in tsUrls!!) {
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

    private fun getTSUrls(playlistUrl: String): List<String>? {
        try {
            val tsUrls: MutableList<String> = ArrayList()
            val url = URL(playlistUrl)
            val s = Scanner(url.openStream())
            while (s.hasNext()) {
                val line: String = s.next()
                if (!line.endsWith(".ts")) continue
                tsUrls.add(line)
            }
            return tsUrls
        } catch (ignored: java.lang.Exception) {
        }
        return null
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