package org.hugoandrade.rtpplaydownloader.network

import android.util.Log
import java.io.File
import java.net.URL


class DownloadableItem(urlText: String, viewOps: DownloadManager.DownloadManagerViewOps?) :
        DownloaderTaskListener,
        DownloadableItemStateChangeSupport {

    enum class State {
        Start,
        Downloading,
        End
    }

    var TAG : String = javaClass.simpleName

    private val urlText: String = urlText
    private val viewOps : DownloadManager.DownloadManagerViewOps? = viewOps

    private val mFileIdentifier : FileIdentifier = FileIdentifier()
    private var downloaderTask: DownloaderTaskBase? = null

    var state: State = State.Start
    var filename: String? = null
    var filepath: String? = null
    var progress : Float = 0f;

    private val listenerSet : HashSet<DownloadableItemStateChangeListener>  = HashSet<DownloadableItemStateChangeListener>()

    fun start(): DownloadableItem {

        val isUrl = isValidURL(urlText);

        if (!isUrl) {
            this.state = State.End
            fireDownloadStateChange()
            viewOps?.onParsingEnded(urlText, false, "is not a valid website");
            return this
        }

        object : Thread() {
            override fun run() {

                downloaderTask = mFileIdentifier.findHost(urlText);

                val downloading : Boolean? = downloaderTask?.downloadAsync(this@DownloadableItem, urlText)

                if (downloading == null || !downloading) {
                    this@DownloadableItem.state = DownloadableItem.State.End
                    fireDownloadStateChange()
                    viewOps?.onParsingEnded(urlText, false, "could not find filetype")
                }
                else {
                    this@DownloadableItem.state = DownloadableItem.State.Downloading
                    fireDownloadStateChange()
                }
            }
        }.start()
        return this
    }

    override fun onProgress(progress: Float) {
        this.progress = progress
        fireDownloadStateChange()
        viewOps?.onDownloading(progress);
    }

    override fun downloadFinished(f: File) {
        this.filepath = f.absolutePath
        this.filename = f.name
        this.state = State.End
        Log.e(TAG, "finished downloading to " + f.absolutePath)
        fireDownloadStateChange()
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

    private fun fireDownloadStateChange() {
        synchronized(listenerSet) {
            listenerSet.forEach(action = { it.onDownloadStateChange(this@DownloadableItem) })
        }
    }
}