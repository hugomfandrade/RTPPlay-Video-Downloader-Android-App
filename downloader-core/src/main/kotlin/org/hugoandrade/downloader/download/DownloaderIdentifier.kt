package org.hugoandrade.downloader.download

import org.hugoandrade.downloader.dev.DevDownloaderTask
import org.hugoandrade.downloader.DownloadableItem

class DownloaderIdentifier {

    init {
        throw AssertionError()
    }

    companion object {

        const val TAG = "DownloaderIdentifier"

        @Throws(IllegalAccessException::class)
        fun findTask(dir: String, downloadableItem: DownloadableItem, listener : DownloaderTask.Listener): DownloaderTask {

            val mediaUrl = downloadableItem.mediaUrl ?: throw IllegalAccessException("mediaUrl not found")
            val filename = downloadableItem.filename ?: throw IllegalAccessException("filename not found")

            if (mediaUrl.contains("dev.com")) {
                return DevDownloaderTask(listener);
            }
            if (mediaUrl.contains(".m3u8")) {
                return TSDownloaderTask(mediaUrl, dir, filename, listener)
            }
            else {
                return RawDownloaderTask(mediaUrl, dir, filename, listener)
            }
        }
    }
}