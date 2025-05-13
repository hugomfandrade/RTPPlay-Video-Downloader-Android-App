package org.hugoandrade.rtpplaydownloader.network.download

import android.net.Uri
import org.hugoandrade.downloader.download.DownloaderIdentifier
import org.hugoandrade.downloader.download.DownloaderTask
import org.hugoandrade.rtpplaydownloader.network.AndroidDownloadableItem

@Deprecated("should avoid to use this. use instead class in core module")
class AndroidDownloaderIdentifier {

    init {
        throw AssertionError()
    }

    companion object {

        const val TAG = "DownloaderIdentifierAndroid"

        @Throws(IllegalAccessException::class)
        fun findTask(dirPath: Uri, downloadableItem: AndroidDownloadableItem): DownloaderTask {
            return DownloaderIdentifier.findTask(dirPath.toString(), downloadableItem, downloadableItem)
        }
    }
}