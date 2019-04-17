package org.hugoandrade.rtpplaydownloader.network

abstract class DownloaderTaskBase {

    var TAG : String = javaClass.simpleName

    var videoFile: String? = null
    var videoFileName: String? = null
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

    fun downloadVideoFileAsync(listener: DownloaderTaskListener, videoFile: String?, videoFileName: String?) : Boolean {
        if (isDownloading) {
            return false
        }

        isDownloading = true

        object : Thread("Thread_download_video_file_" + videoFile) {
            override fun run() {

                downloadVideoFile(listener, videoFile, videoFileName)
                isDownloading = false
            }
        }.start()

        return true
    }

    abstract fun parseMediaFile(urlString: String): Boolean

    protected abstract fun download(listener: DownloaderTaskListener, urlString: String)
    protected abstract fun downloadVideoFile(listener: DownloaderTaskListener, videoFile: String?, videoFileName: String?)

    abstract fun cancel()
    abstract fun resume()
    abstract fun pause()
}