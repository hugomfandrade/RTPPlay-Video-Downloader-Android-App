package org.hugoandrade.rtpplaydownloader.network

import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTask
import java.io.File

class DownloadableItemAction(val item : DownloadableItem,
                             internal val downloadTask: DownloaderTask) :

        DownloadableItemActionAPI,
        DownloaderTask.Listener {

    private val TAG : String = javaClass.simpleName

    override fun startDownload() {

        val downloading : Boolean = downloadTask.isDownloading()

        if (downloading) {
            item.state = DownloadableItem.State.Failed

            item.fireDownloadStateChange()

            item.downloadFailed("task is currently downloading")
        }
        else {
            item.progress = 0.0f
            item.progressSize = 0
            item.state = DownloadableItem.State.Downloading

            item.fireDownloadStateChange()
        }
    }

    override fun cancel() {
        downloadTask.cancel()
        item.state = DownloadableItem.State.Failed
        item.fireDownloadStateChange()
    }

    override fun resume() {
        downloadTask.resume()
        item.state = DownloadableItem.State.Downloading
        item.fireDownloadStateChange()
    }

    override fun pause() {
        downloadTask.pause()
        item.state = DownloadableItem.State.Paused
        item.fireDownloadStateChange()
    }

    override fun refresh() {
        actionListener.forEach { l -> l.onRefresh(this)}
        item.state = DownloadableItem.State.Start
        item.fireDownloadStateChange()
    }

    private val actionListener: HashSet<Listener> = HashSet()

    fun addActionListener(listener: Listener) {
        if (!actionListener.contains(listener)) {
            actionListener.add(listener)
        }
    }

    override fun play() {
        actionListener.forEach { l -> l.onPlay(this)}
    }

    fun isDownloading(): Boolean {
        return downloadTask.isDownloading()
    }

    fun isResumed(): Boolean {
        return downloadTask.isResuming()
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

    interface Listener {
        fun onPlay(action : DownloadableItemAction)
        fun onRefresh(action: DownloadableItemAction)
    }

    fun addDownloadStateChangeListener(listener: DownloadableItem.State.ChangeListener) {
        item.addDownloadStateChangeListener(listener)
    }

    fun removeDownloadStateChangeListener(listener: DownloadableItem.State.ChangeListener) {
        item.removeDownloadStateChangeListener(listener)
    }
}