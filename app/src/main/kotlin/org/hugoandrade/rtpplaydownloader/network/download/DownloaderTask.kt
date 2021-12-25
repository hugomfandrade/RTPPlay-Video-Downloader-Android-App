package org.hugoandrade.rtpplaydownloader.network.download

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.locks.ReentrantLock

abstract class DownloaderTask(private val listener : Listener) {

    open val TAG : String = javaClass.simpleName

    var isDownloading : Boolean = false
    var doCanceling: Boolean = false

    abstract fun downloadMediaFile()

    protected fun tryToCancelIfNeeded(fileOutputStream: FileOutputStream,
                                      inputStream: InputStream,
                                      file: File): Boolean {

        if (doCanceling) {
            try {
                fileOutputStream.close()
            } catch (ioe: IOException) {
                // just going to ignore this one
            }
            try {
                inputStream.close()
            } catch (ioe: IOException) {
                // just going to ignore this one
            }

            file.delete()

            dispatchDownloadFailed("cancelled")
            isDownloading = false
            doCanceling = false
            return true
        }
        return false
    }

    fun cancel() {
        doCanceling = true
    }

    val lock : ReentrantLock = ReentrantLock()

    fun resume() {
        isDownloading = true
        if (lock.holdCount != 0) {
            lock.unlock()
        }
    }

    fun pause() {
        isDownloading = false
        lock.lock()
    }

    fun dispatchProgress(downloadedSize: Long, totalSize: Long) {
        listener.onProgress(downloadedSize, totalSize)
    }

    fun dispatchDownloadStarted(f: File) {
        listener.downloadStarted(f)
    }

    fun dispatchDownloadFinished(f: File) {
        listener.downloadFinished(f)
    }

    fun dispatchDownloadFailed(message: String?) {
        listener.downloadFailed(message)
    }

    interface Listener {

        fun downloadStarted(f: File)
        fun onProgress(downloadedSize: Long, totalSize: Long)
        fun downloadFinished(f: File)
        fun downloadFailed(message: String?)
    }
}