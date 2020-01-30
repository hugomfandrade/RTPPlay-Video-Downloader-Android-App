package org.hugoandrade.rtpplaydownloader.network.download

import android.os.Build
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL

open class DownloaderTask(private val mediaUrl : String,
                          private val dirPath : String,
                          private val filename : String,
                          private val listener : DownloaderTaskListener) {

    open val TAG : String = javaClass.simpleName

    var isDownloading : Boolean = false
    var doCanceling: Boolean = false

    open fun downloadMediaFile() {

        if (isDownloading) {
            return
        }

        isDownloading = true
        doCanceling = false

        val u: URL?
        var inputStream: InputStream? = null

        try {
            try {
                u = URL(mediaUrl)
            }
            catch (e: Exception) {
                listener.downloadFailed("URL no longer exists")
                return
            }
            inputStream = u.openStream()
            val huc = u.openConnection() as HttpURLConnection //to know the size of video
            val size = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                huc.contentLengthLong
            } else {
                huc.contentLength.toLong()
            }

            val storagePath = dirPath
            val f = File(storagePath, filename)
            if (MediaUtils.doesMediaFileExist(f)) {
                isDownloading = false
                listener.downloadFailed("file with same name already exists")
                return
            }
            listener.downloadStarted(f)

            val fos = FileOutputStream(f)
            val buffer = ByteArray(1024)
            if (inputStream != null) {
                var len = inputStream.read(buffer)
                var progress = len.toLong()
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

                    listener.onProgress(progress, size)
                }
            }
            listener.downloadFinished(f)

            fos.close()

        } catch (ioe: IOException) {
            ioe.printStackTrace()
            listener.downloadFailed("Internal error while downloading")
        } finally {
            try {
                inputStream?.close()
            } catch (ioe: IOException) {
                // just going to ignore this one
            }
        }
        isDownloading = false
    }

    open fun cancel() {
        doCanceling = true
    }

    open fun resume() {
        isDownloading = true
    }

    open fun pause() {
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