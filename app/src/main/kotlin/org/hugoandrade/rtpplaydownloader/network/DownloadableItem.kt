package org.hugoandrade.rtpplaydownloader.network

import android.util.Log
import org.hugoandrade.rtpplaydownloader.network.parsing.FileIdentifier
import java.io.File

class DownloadableItem(private val urlText: String, private val viewOps: DownloadManagerViewOps?) :
        DownloaderTaskListener,
        DownloadableItemStateChangeSupport {

    constructor(task: DownloaderTaskBase, viewOps: DownloadManagerViewOps?) : this("", viewOps) {
        downloaderTask = task
    }

    val TAG : String = javaClass.simpleName

    private var downloaderTask: DownloaderTaskBase? = null

    var state: DownloadableItemState = DownloadableItemState.Start
    var filename: String? = null
    var filepath: String? = null
    var progress : Float = 0f

    private val listenerSet : HashSet<DownloadableItemStateChangeListener>  = HashSet()

    fun startDownload(): DownloadableItem {

        object : Thread() {
            override fun run() {

                val downloading : Boolean? = downloaderTask?.downloadVideoFileAsync(
                        this@DownloadableItem,
                        downloaderTask?.videoFile,
                        downloaderTask?.videoFileName)

                if (downloading == null || !downloading) {
                    this@DownloadableItem.state = DownloadableItemState.End
                    fireDownloadStateChange()
                    viewOps?.onParsingError(urlText, "could not find filetype")
                }
                else {
                    viewOps?.onParsingSuccessful(this@DownloadableItem)
                    this@DownloadableItem.state = DownloadableItemState.Downloading
                    fireDownloadStateChange()
                }
            }
        }.start()
        return this
    }

    fun start(): DownloadableItem {

        val isUrl : Boolean = NetworkUtils.isValidURL(urlText)

        if (!isUrl) {
            this.state = DownloadableItemState.End
            fireDownloadStateChange()
            viewOps?.onParsingError(urlText, "is not a valid website")
            return this
        }

        object : Thread() {
            override fun run() {

                downloaderTask = FileIdentifier.findHost(urlText)

                val downloading : Boolean? = downloaderTask?.downloadAsync(this@DownloadableItem, urlText)

                if (downloading == null || !downloading) {
                    this@DownloadableItem.state = DownloadableItemState.End
                    fireDownloadStateChange()
                    viewOps?.onParsingError(urlText, "could not find filetype")
                }
                else {
                    viewOps?.onParsingSuccessful(this@DownloadableItem)
                    this@DownloadableItem.state = DownloadableItemState.Downloading
                    fireDownloadStateChange()
                }
            }
        }.start()
        return this
    }

    fun cancel() {
        downloaderTask?.cancel()
        state = DownloadableItemState.End
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
        if (checkNotNull(downloaderTask).isDownloading) {
            cancel()
        }
        start()
    }

    fun isDownloading(): Boolean {
        val state = checkNotNull(downloaderTask)
        return state.isDownloading
    }

    override fun onProgress(progress: Float) {
        this.progress = progress
        this.state = DownloadableItemState.Downloading
        fireDownloadStateChange()
    }

    override fun downloadStarted(f: File) {
        this.filepath = f.absolutePath
        this.filename = f.name
        this.state = DownloadableItemState.Start
        Log.e(TAG, "start downloading to " + f.absolutePath)
        fireDownloadStateChange()
    }

    override fun downloadFinished(f: File) {
        this.filepath = f.absolutePath
        this.filename = f.name
        this.state = DownloadableItemState.End
        Log.e(TAG, "finished downloading to " + f.absolutePath)
        fireDownloadStateChange()
    }

    override fun downloadFailed() {
        this.state = DownloadableItemState.Start
        fireDownloadStateChange()
        Log.e(TAG, "failed to download " + filepath)
        viewOps?.onParsingError(urlText, "failed to download " + filepath)
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