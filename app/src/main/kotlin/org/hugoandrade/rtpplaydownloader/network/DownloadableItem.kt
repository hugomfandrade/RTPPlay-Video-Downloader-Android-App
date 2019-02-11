package org.hugoandrade.rtpplaydownloader.network

import android.util.Log
import java.io.File
import java.net.URL


class DownloadableItem(urlText: String, viewOps: DownloadManager.DownloadManagerViewOps?) :
        DownloaderTaskListener,
        DownloadableItemStateChangeSupport {

    var TAG : String = javaClass.simpleName

    private val urlText: String = urlText
    private val viewOps : DownloadManager.DownloadManagerViewOps? = viewOps

    private val mFileIdentifier : FileIdentifier = FileIdentifier()
    private var downloaderTask: DownloaderTaskBase? = null

    private val filename: String? = null
    private val filepath: String? = null

    private val listenerSet : HashSet<DownloadableItemStateChangeListener>  = HashSet<DownloadableItemStateChangeListener>()

    fun start(): DownloadableItem {

        val isUrl = isValidURL(urlText);

        if (!isUrl) {
            synchronized(listenerSet) { listenerSet.forEach(action = { it.onDownloadStateChange(this) }) }
            viewOps?.onParsingEnded(urlText, false, "is not a valid website");
            return this
        }

        object : Thread() {
            override fun run() {

                downloaderTask = mFileIdentifier.findHost(urlText);

                val downloading : Boolean? = downloaderTask?.downloadAsync(this@DownloadableItem, urlText)

                if (downloading == null || !downloading) {
                    synchronized(listenerSet) { listenerSet.forEach(action = { it.onDownloadStateChange(this@DownloadableItem) }) }
                    viewOps?.onParsingEnded(urlText, false, "could not find filetype")
                }
            }
        }.start()
        return this
    }

    override fun onProgress(progress: Float) {
        synchronized(listenerSet) { listenerSet.forEach(action = { it.onDownloadStateChange(this@DownloadableItem) }) }
        viewOps?.onDownloading(progress);
    }

    override fun downloadFinished(f: File) {
        Log.e(TAG, "finished downloading to " + f.absolutePath);
        synchronized(listenerSet) { listenerSet.forEach(action = { it.onDownloadStateChange(this@DownloadableItem) }) }
        viewOps?.onParsingEnded(f.absolutePath, true, "finished downloading");
    }

    private fun isValidURL(urlText: String): Boolean {
        try {
            val url = URL(urlText);
            return "http".equals(url.getProtocol()) || "https".equals(url.getProtocol());
        } catch (e : Exception) {
            return false;
        }
    }

    override fun addDownloadStateChangeListener(downloadableItemStateChangeListener: DownloadableItemStateChangeListener) {
        synchronized(listenerSet) {
            listenerSet.add(downloadableItemStateChangeListener)
        }
    }

    override fun removeDownloadStateChangeListener(downloadableItemStateChangeListener: DownloadableItemStateChangeListener) {
        synchronized(listenerSet) {
            listenerSet.remove(downloadableItemStateChangeListener)
        }
    }
}