package org.hugoandrade.rtpplaydownloader.network

import android.util.Log
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTaskBase
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTaskListener
import java.io.File

class DownloadableItem(private val downloaderTask: DownloaderTaskBase,
                       private val viewOps: DownloadManagerViewOps?) :
        DownloaderTaskListener,
        DownloadableItemStateChangeSupport {

    @Suppress("PrivatePropertyName")
    private val TAG : String = javaClass.simpleName

    var state: DownloadableItemState = DownloadableItemState.Start
    var filename: String? = null
    var filepath: String? = null
    var progress : Float = 0f

    private val listenerSet : HashSet<DownloadableItemStateChangeListener>  = HashSet()

    fun startDownload(): DownloadableItem {

        object : Thread() {
            override fun run() {

                val downloading : Boolean = downloaderTask.downloadMediaFileAsync(this@DownloadableItem)

                if (!downloading) {
                    this@DownloadableItem.state = DownloadableItemState.End
                    fireDownloadStateChange()
                    viewOps?.onParsingError(checkNotNull(downloaderTask.videoFile), "could not find filetype")
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
        downloaderTask.cancel()
        state = DownloadableItemState.End
        fireDownloadStateChange()
        viewOps?.onParsingError(checkNotNull(downloaderTask.videoFile), "download was cancel")
    }

    fun resume() {
        downloaderTask.resume()
        fireDownloadStateChange()
    }

    fun pause() {
        downloaderTask.pause()
        fireDownloadStateChange()
    }

    fun refresh() {
        if (downloaderTask.isDownloading) {
            cancel()
        }
        startDownload()

    }

    fun isDownloading(): Boolean {
        return downloaderTask.isDownloading
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
        viewOps?.onParsingError(checkNotNull(downloaderTask.videoFile), "failed to download " + filepath)
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