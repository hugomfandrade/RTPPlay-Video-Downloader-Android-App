package org.hugoandrade.rtpplaydownloader.network.download

class EmptyDownloaderTask : DownloaderTaskBase() {

    private lateinit var mDownloaderTaskListener: DownloaderTaskListener

    private var doCanceling: Boolean = false

    override fun cancel() {
        doCanceling = true
    }

    override fun resume() {
        isDownloading = true
    }

    override fun pause() {
        isDownloading = false
    }

    override fun parseMediaFile(urlString: String): Boolean {

        url = urlString
        return false
    }

    override fun downloadMediaFile(listener: DownloaderTaskListener) {

        mDownloaderTaskListener = listener

        mDownloaderTaskListener.downloadFailed()
    }

    override fun isValid(urlString: String) : Boolean {

        return false
    }
}