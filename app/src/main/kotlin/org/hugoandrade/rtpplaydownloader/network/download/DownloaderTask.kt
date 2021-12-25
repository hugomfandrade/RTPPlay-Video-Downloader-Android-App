package org.hugoandrade.rtpplaydownloader.network.download

import org.hugoandrade.rtpplaydownloader.dev.DevConstants
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

abstract class DownloaderTask(private val listener : Listener) : Runnable {

    open val TAG : String = javaClass.simpleName

    @Volatile private var isDownloadingState : Boolean = false
    @Volatile private var isResumingState : Boolean = true
    private var doCanceling: Boolean = false

    fun isDownloading() : Boolean {
        return isDownloadingState
    }

    fun isResuming() : Boolean {
        return isResumingState
    }

    fun doCancelling() {
        doCanceling = true
    }

    fun cancelCancelling() {
        doCanceling = false
    }

    override fun run() {

        if (isDownloadingState) {
            println("already trying to download")
            return
        }

        doCanceling = false
        isDownloadingState = true

        try {
            downloadMediaFile()
        }
        finally {
            isDownloadingState = false
        }
    }

    abstract fun downloadMediaFile()

    protected fun tryToCancelIfNeeded(fileOutputStream: FileOutputStream? = null,
                                      inputStream: InputStream? = null,
                                      file: File? = null): Boolean {

        if (doCanceling) {
            try {
                fileOutputStream?.close()
            } catch (ioe: IOException) {
                ioe.printStackTrace()
            }
            try {
                inputStream?.close()
            } catch (ioe: IOException) {
                ioe.printStackTrace()
            }

            file?.delete()

            dispatchDownloadFailed("cancelled")
            isDownloadingState = false
            doCanceling = false
            return true
        }
        return false
    }

    protected fun doPause() : Boolean {

        while (!isResumingState){
            if (DevConstants.showLog) println("paused")

            // lock to pause
            lock.withLock {}

            // check if it was cancelled while pause
            if (tryToCancelIfNeeded()) return true
        }

        return false
    }

    private val lock : ReentrantLock = ReentrantLock()

    fun cancel() {
        doCanceling = true

        // unlock all
        while (lock.holdCount != 0) {
            lock.unlock()
        }
    }

    fun resume() {
        // check if already on resume state
        if (isResumingState) return

        isResumingState = true
        if (lock.holdCount != 0) {
            lock.unlock()
        }
    }

    fun pause() {
        // check if already on pause resume state
        if (!isResumingState) return

        isResumingState = false
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