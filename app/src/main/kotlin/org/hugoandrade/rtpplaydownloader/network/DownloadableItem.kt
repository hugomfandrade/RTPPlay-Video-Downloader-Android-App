package org.hugoandrade.rtpplaydownloader.network

import android.util.Log
import java.io.File
import java.net.URL

class DownloadableItem(private val urlText: String, private val viewOps: DownloadManagerViewOps?) :
        DownloaderTaskListener,
        DownloadableItemStateChangeSupport {

    enum class State {
        Start,
        Downloading,
        End
    }

    val TAG : String = javaClass.simpleName

    private val mFileIdentifier : FileIdentifier = FileIdentifier()
    private var downloaderTask: DownloaderTaskBase? = null

    var state: State = State.Start
    var filename: String? = null
    var filepath: String? = null
    var progress : Float = 0f

    private val listenerSet : HashSet<DownloadableItemStateChangeListener>  = HashSet()

    fun start(): DownloadableItem {

        val isUrl = isValidURL(urlText)

        if (!isUrl) {
            this.state = State.End
            fireDownloadStateChange()
            viewOps?.onParsingError(urlText, "is not a valid website")
            return this
        }

        object : Thread() {
            override fun run() {

                downloaderTask = mFileIdentifier.findHost(urlText)

                val downloading : Boolean? = downloaderTask?.downloadAsync(this@DownloadableItem, urlText)

                if (downloading == null || !downloading) {
                    this@DownloadableItem.state = DownloadableItem.State.End
                    fireDownloadStateChange()
                    viewOps?.onParsingError(urlText, "could not find filetype")
                }
                else {
                    viewOps?.onParsingSuccessful(this@DownloadableItem)
                    this@DownloadableItem.state = DownloadableItem.State.Downloading
                    fireDownloadStateChange()
                }
            }
        }.start()
        return this
    }

    fun cancel() {
        downloaderTask?.cancel()
        state = State.End
        fireDownloadStateChange()
        viewOps?.onParsingError(urlText, "download was cancel")
    }

    fun resume() {
        downloaderTask?.resume()
        fireDownloadStateChange()
    }

    fun pause() {
        downloaderTask?.pause()
        fireDownloadStateChange()
    }

    fun refresh() {
        cancel()
        start()
    }

    fun isDownloading(): Boolean {
        val state = checkNotNull(downloaderTask)
        return state.isDownloading
    }

    override fun onProgress(progress: Float) {
        this.progress = progress
        this.state = DownloadableItem.State.Downloading
        fireDownloadStateChange()
    }

    override fun downloadStarted(f: File) {
        this.filepath = f.absolutePath
        this.filename = f.name
        this.state = State.Start
        Log.e(TAG, "start downloading to " + f.absolutePath)
        fireDownloadStateChange()
    }

    override fun downloadFinished(f: File) {
        this.filepath = f.absolutePath
        this.filename = f.name
        this.state = State.End
        Log.e(TAG, "finished downloading to " + f.absolutePath)
        fireDownloadStateChange()
    }

    override fun downloadFailed() {
        this.state = State.End
        fireDownloadStateChange()
        Log.e(TAG, "failed to download " + filepath)
        viewOps?.onParsingError(urlText, "failed to download " + filepath)
    }

    private fun isValidURL(urlText: String): Boolean {
        return try {
            val url = URL(urlText)
            "http" == url.protocol || "https" == url.protocol
        } catch (e : Exception) {
            false
        }
    }

    @Volatile
    private var isFiring = false

    private val tmpAddListenerSet : HashSet<DownloadableItemStateChangeListener>  = HashSet()
    private val tmpRemoveListenerSet : HashSet<DownloadableItemStateChangeListener>  = HashSet()

    override fun addDownloadStateChangeListener(downloadableItemStateChangeListener: DownloadableItemStateChangeListener) {
        if (isFiring) {
            synchronized(tmpAddListenerSet) {
                tmpAddListenerSet.add(downloadableItemStateChangeListener)
            }
        }
        else {
            synchronized(listenerSet) {
                listenerSet.add(downloadableItemStateChangeListener)
            }
        }
    }

    override fun removeDownloadStateChangeListener(downloadableItemStateChangeListener: DownloadableItemStateChangeListener) {
        if (isFiring) {
            synchronized(tmpRemoveListenerSet) {
                tmpRemoveListenerSet.remove(downloadableItemStateChangeListener)
            }
        }
        else {
            synchronized(listenerSet) {
                listenerSet.remove(downloadableItemStateChangeListener)
            }
        }
    }

    private fun fireDownloadStateChange() {
        isFiring = true
        synchronized(listenerSet) {
            listenerSet.forEach(action = { it.onDownloadStateChange(this@DownloadableItem) })
        }
        isFiring = false
        addTmpListeners()
        removeTmpListeners()
    }

    private fun addTmpListeners() {
        synchronized(tmpAddListenerSet) {
            synchronized(listenerSet) {
                tmpAddListenerSet.forEach(action = { listenerSet.add(it) })
                tmpAddListenerSet.clear()
            }
        }
    }

    private fun removeTmpListeners() {
        synchronized(tmpRemoveListenerSet) {
            synchronized(listenerSet) {
                tmpRemoveListenerSet.forEach(action = { listenerSet.remove(it) })
                tmpRemoveListenerSet.clear()
            }
        }
    }
}