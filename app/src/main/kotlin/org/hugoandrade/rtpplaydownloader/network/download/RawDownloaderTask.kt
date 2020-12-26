package org.hugoandrade.rtpplaydownloader.network.download

import android.os.Build
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import java.net.URL

class RawDownloaderTask(private val mediaUrl : String,
                        private val dirPath : String,
                        private val filename : String,
                        private val listener : Listener) :

        DownloaderTask(listener) {

    override fun downloadMediaFile() {

        if (isDownloading) return

        isDownloading = true
        doCanceling = false

        var mInputStream: InputStream? = null

        try {
            val url : URL
            try {
                url = URL(mediaUrl)
            }
            catch (e: Exception) {
                isDownloading = false
                dispatchDownloadFailed("URL no longer exists")
                return
            }
            val inputStream = url.openStream()
            mInputStream = inputStream

            val huc = url.openConnection()
            val size = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                huc.contentLengthLong
            } else {
                huc.contentLength.toLong()
            }

            val storagePath = dirPath
            val f = File(storagePath, filename)
            if (MediaUtils.doesMediaFileExist(f)) {
                isDownloading = false
                dispatchDownloadFailed("file with same name already exists")
                return
            }
            dispatchDownloadStarted(f)

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

                    dispatchProgress(progress, size)
                }
            }
            dispatchDownloadFinished(f)

            fos.close()

        } catch (ioe: IOException) {
            ioe.printStackTrace()
            dispatchDownloadFailed("Internal error while downloading")
        } finally {
            isDownloading = false
            try {
                mInputStream?.close()
            } catch (ioe: IOException) {
                // just going to ignore this one
            }
        }
    }
}