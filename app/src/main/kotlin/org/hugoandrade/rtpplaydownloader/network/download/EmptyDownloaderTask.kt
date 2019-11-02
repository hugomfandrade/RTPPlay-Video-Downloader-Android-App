package org.hugoandrade.rtpplaydownloader.network.download

class EmptyDownloaderTask : DownloaderTaskBase() {

    override fun getMediaFileName(urlString: String, videoFile: String?): String {

        return null.toString()
    }

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

        mDownloaderTaskListener.downloadFailed("No task found")
    }

    override fun isValid(urlString: String) : Boolean {

        return false
    }
}