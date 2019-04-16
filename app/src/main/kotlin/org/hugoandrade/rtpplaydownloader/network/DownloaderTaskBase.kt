package org.hugoandrade.rtpplaydownloader.network

abstract class DownloaderTaskBase {

    var TAG : String = javaClass.simpleName

    var isDownloading : Boolean = false

    abstract fun isValid(urlString: String) : Boolean

    // abstract fun findDownloadableFile(urlString: String)

    fun downloadAsync(listener: DownloaderTaskListener, urlString: String) : Boolean {
        if (isDownloading) {
            return false
        }

        isDownloading = true

        object : Thread("Thread_download_" + urlString) {
            override fun run() {

                download(listener, urlString)
                isDownloading = false
            }
        }.start()

        return true
    }

    abstract fun parseMediaFile(urlString: String): Boolean

    protected abstract fun download(listener: DownloaderTaskListener, urlString: String)

    abstract fun cancel()
    abstract fun resume()
    abstract fun pause()
}