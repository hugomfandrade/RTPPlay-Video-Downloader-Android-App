package org.hugoandrade.rtpplaydownloader.network.download

import android.net.Uri
import org.hugoandrade.rtpplaydownloader.dev.download.DevDownloaderTask
import org.hugoandrade.rtpplaydownloader.network.DownloadableItem

class DownloaderIdentifier {

    init {
        throw AssertionError()
    }

    companion object {

        val TAG = "DownloaderIdentifier"

        @Throws(IllegalAccessException::class)
        fun findTask(dirPath: Uri, downloadableItem: DownloadableItem): DownloaderTask {
            return findTask(dirPath.toString(), downloadableItem, downloadableItem)
        }

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