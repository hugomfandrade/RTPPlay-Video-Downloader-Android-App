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

        object : Thread() {
            override fun run() {

                download(listener, urlString)
                isDownloading = false
            }
        }.start()

        return true
    }

    protected abstract fun download(listener: DownloaderTaskListener, urlString: String)
}