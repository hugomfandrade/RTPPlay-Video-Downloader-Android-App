package org.hugoandrade.rtpplaydownloader.network

import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTask
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTaskListener
import java.io.File

class DownloadableItemAction(val item : DownloadableItem,
                             internal val downloadTask: DownloaderTask) :

        IDownloadableItemAction,
        DownloaderTaskListener {

    private val TAG : String = javaClass.simpleName

    override fun startDownload() {

        val downloading : Boolean = downloadTask.isDownloading

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
        downloadTask.cancel()
        item.state = DownloadableItemState.Failed
        item.fireDownloadStateChange()
    }

    override fun resume() {
        downloadTask.resume()
        item.fireDownloadStateChange()
    }

    override fun pause() {
        downloadTask.pause()
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
        actionListener.forEach { l -> l.onPlay(this)}
    }

    fun isDownloading(): Boolean {
        return downloadTask.isDownloading
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