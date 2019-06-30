package org.hugoandrade.rtpplaydownloader.network.download

import android.os.Build
import android.os.Environment
import org.hugoandrade.rtpplaydownloader.DevConstants
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

abstract class DownloaderTaskBase {

    val TAG : String = javaClass.simpleName

    companion object {
        const val DOWNLOAD_SPEED_CALCULATION_TIMESPAN_IN_MILLIS : Long = 1000 // 1second
    }

    var videoFile: String? = null
    var videoFileName: String? = null
    var isDownloading : Boolean = false

    lateinit var mDownloaderTaskListener: DownloaderTaskListener

    var doCanceling: Boolean = false

    abstract fun isValid(urlString: String) : Boolean

    abstract fun parseMediaFile(urlString: String): Boolean

    abstract fun getVideoFileName(urlString: String, videoFile: String?): String

    fun downloadMediaFileAsync(listener: DownloaderTaskListener) : Boolean {
        if (isDownloading) {
            return false
        }

        isDownloading = true

        object : Thread("Thread_download_media_file_" + videoFile) {
            override fun run() {

                downloadMediaFile(listener)
                isDownloading = false
            }
        }.start()

        return true
    }

    open fun downloadMediaFile(listener: DownloaderTaskListener) {

        isDownloading = true

        mDownloaderTaskListener = listener

        if (DevConstants.simDownload) {
            val f = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString(), videoFileName)
            mDownloaderTaskListener.downloadStarted(f)
            var progress = 0L
            var size = 1024L
            while (progress < size) {
                if (doCanceling) {
                    mDownloaderTaskListener.downloadFailed("Downloading was cancelled")
                    isDownloading = false
                    doCanceling = false
                    return
                }
                while (!isDownloading){
                    // paused

                    if (doCanceling) {
                        mDownloaderTaskListener.downloadFailed("Downloading was cancelled")
                        isDownloading = false
                        doCanceling = false
                        return
                    }
                }
                mDownloaderTaskListener.onProgress(1024F, 3000)
                mDownloaderTaskListener.onProgress(progress.toFloat() / size.toFloat())
                mDownloaderTaskListener.onProgress(progress, size)
                progress += 8
                Thread.sleep(1000)
            }
            mDownloaderTaskListener.downloadFinished(f)
            isDownloading = false
            return
        }

        val u: URL?
        var inputStream: InputStream? = null

        try {
            u = URL(videoFile)
            inputStream = u.openStream()
            val huc = u.openConnection() as HttpURLConnection //to know the size of video
            val size = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                huc.contentLengthLong
            } else {
                huc.contentLength.toLong()
            }

            val storagePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString()
            val f = File(storagePath, videoFileName)
            if (MediaUtils.doesMediaFileExist(f)) {
                isDownloading = false
                mDownloaderTaskListener.downloadFailed("file with same name already exists")
                return
            }
            mDownloaderTaskListener.downloadStarted(f)

            val fos = FileOutputStream(f)
            val buffer = ByteArray(1024)
            if (inputStream != null) {
                var len = inputStream.read(buffer)
                var progress = len.toLong()
                var oldTimestamp = System.currentTimeMillis()
                var oldProgress = len.toLong()
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
                        oldTimestamp = System.currentTimeMillis()
                        oldProgress = len.toLong()
                    }

                    fos.write(buffer, 0, len)
                    len = inputStream.read(buffer)
                    progress += len

                    if (tryToCancelIfNeeded(fos, inputStream, f)) {
                        // do cancelling while paused
                        return
                    }

                    val tmpTimestamp: Long = System.currentTimeMillis()
                    if ((tmpTimestamp - oldTimestamp) >= DOWNLOAD_SPEED_CALCULATION_TIMESPAN_IN_MILLIS) {
                        val downloadingSpeedPerSecond : Float = MediaUtils.calculateDownloadingSpeed(oldTimestamp, tmpTimestamp, oldProgress, progress)
                        val remainingTimeInMillis: Long = MediaUtils.calculateRemainingDownloadTime(oldTimestamp, tmpTimestamp, oldProgress, progress, size)
                        mDownloaderTaskListener.onProgress(downloadingSpeedPerSecond, remainingTimeInMillis)
                        oldTimestamp = tmpTimestamp
                        oldProgress = progress
                    }

                    mDownloaderTaskListener.onProgress(progress.toFloat() / size.toFloat())
                    mDownloaderTaskListener.onProgress(progress, size)
                }
            }
            mDownloaderTaskListener.downloadFinished(f)

            fos.close()

        } catch (mue: MalformedURLException) {
            mue.printStackTrace()
            mDownloaderTaskListener.downloadFailed("Internal error while downloading")
        } catch (ioe: IOException) {
            ioe.printStackTrace()
            mDownloaderTaskListener.downloadFailed("Internal error while downloading")
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

            mDownloaderTaskListener.downloadFailed("Downloading was aborted")
            isDownloading = false
            doCanceling = false
            return true
        }
        return false
    }
}