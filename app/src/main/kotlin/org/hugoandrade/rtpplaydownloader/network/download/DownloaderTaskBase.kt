package org.hugoandrade.rtpplaydownloader.network.download

abstract class DownloaderTaskBase {

    var TAG : String = javaClass.simpleName

    var videoFile: String? = null
    var videoFileName: String? = null
    var isDownloading : Boolean = false

    abstract fun isValid(urlString: String) : Boolean

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

    protected abstract fun downloadMediaFile(listener: DownloaderTaskListener)
    abstract fun parseMediaFile(urlString: String): Boolean
    abstract fun cancel()
    abstract fun resume()
    abstract fun pause()
}