package org.hugoandrade.rtpplaydownloader.network

import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTaskBase
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTaskListener
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import java.io.File
import java.util.concurrent.ExecutorService

class DownloadableItemAction(val item : DownloadableItem,
                             internal val downloaderTask: DownloaderTaskBase) :

        IDownloadableItemAction,
        DownloaderTaskListener {

    private val TAG : String = javaClass.simpleName

    override fun startDownload() {

        val downloading : Boolean = downloaderTask.isDownloading

        if (downloading) {
            item.state = DownloadableItemState.Failed

            item.fireDownloadStateChange()

            item.downloadFailed("task is currently downloading")
        }
        else {
            item.progress = 0.0f
            item.progressSize = 0
            item.state = DownloadableItemState.Downloading

            item.fireDownloadStateChange()
        }
    }

    override fun cancel() {
        downloaderTask.cancel()
        item.state = DownloadableItemState.Failed
        item.fireDownloadStateChange()
    }

    override fun resume() {
        downloaderTask.resume()
        item.fireDownloadStateChange()
    }

    override fun pause() {
        downloaderTask.pause()
        item.fireDownloadStateChange()
    }

    override fun refresh() {
        actionListener.forEach { l -> l.onRefresh(this)}
    }

    private val actionListener: HashSet<DownloadableItemActionListener> = HashSet()

    fun addActionListener(listener: DownloadableItemActionListener) {
        if (!actionListener.contains(listener)) {
            actionListener.add(listener)
        }
    }

    override fun play() {
        if (!downloaderTask.isDownloading && item.state == DownloadableItemState.End) {
            actionListener.forEach { l -> l.onPlay(this)}
        }
    }

    fun isDownloading(): Boolean {
        return downloaderTask.isDownloading
    }

    override fun onProgress(downloadedSize: Long, totalSize : Long) {
        item.onProgress(downloadedSize, totalSize)

        // fireDownloadStateChange()
    }

    override fun downloadStarted(f: File) {
        item.downloadStarted(f)
    }

    override fun downloadFinished(f: File) {
        item.downloadFinished(f)
    }

    override fun downloadFailed(message: String?) {
        item.downloadFailed(message)
    }
}