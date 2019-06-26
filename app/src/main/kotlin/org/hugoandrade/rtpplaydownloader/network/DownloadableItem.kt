package org.hugoandrade.rtpplaydownloader.network

import android.util.Log
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTaskBase
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTaskListener
import java.io.File
import java.util.*
import kotlin.collections.HashSet
import android.content.Intent
import android.net.Uri
import org.hugoandrade.rtpplaydownloader.network.download.EmptyDownloaderTask
import org.hugoandrade.rtpplaydownloader.network.parsing.FileIdentifier
import org.hugoandrade.rtpplaydownloader.network.parsing.FileType

class DownloadableItem(private val downloaderTask: DownloaderTaskBase,
                       private val viewOps: DownloadManagerViewOps?) :
        DownloaderTaskListener,
        DownloadableItemStateChangeSupport {

    constructor(id: String?,
                url: String?,
                filename: String?,
                filepath: String?,
                state: DownloadableItemState?,
                isArchived: Boolean?)
            : this(FileIdentifier.findHost(url ?: "unknown")?: EmptyDownloaderTask(), null) {

        this.id = id
        this.url = url
        this.filename = filename
        this.filepath = filepath
        this.state = state ?: DownloadableItemState.Start
        this.progress = if (this.state == DownloadableItemState.End) 1f else 0f
        this.isArchived = isArchived ?: false
    }

    @Suppress("PrivatePropertyName")
    private val TAG : String = javaClass.simpleName

    var id: String? = null
    var url: String? = null
    var filename: String? = null
    var filepath: String? = null
    var state: DownloadableItemState = DownloadableItemState.Start
    var progress : Float = 0f
    var isArchived : Boolean = false

    private val listenerSet : HashSet<DownloadableItemStateChangeListener>  = HashSet()

    fun startDownload(): DownloadableItem {

        object : Thread() {
            override fun run() {

                url = downloaderTask.url

                val downloading : Boolean = downloaderTask.downloadMediaFileAsync(this@DownloadableItem)

                if (!downloading) {
                    this@DownloadableItem.state = DownloadableItemState.Failed

                    stopRefreshTimer()

                    fireDownloadStateChange()
                    viewOps?.onParsingError(checkNotNull(downloaderTask.videoFile), "could not find filetype")
                }
                else {
                    viewOps?.onParsingSuccessful(this@DownloadableItem)
                    this@DownloadableItem.progress = 0.0f
                    this@DownloadableItem.state = DownloadableItemState.Downloading

                    startRefreshTimer()

                    fireDownloadStateChange()
                }
            }
        }.start()
        return this
    }

    fun cancel() {
        downloaderTask.cancel()
        state = DownloadableItemState.Failed
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

    fun play() {
        if (!downloaderTask.isDownloading
                && state == DownloadableItemState.End
                && filepath != null) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(filepath))
            intent.setDataAndType(Uri.parse(filepath), "video/mp4")
            viewOps?.getApplicationContext()?.startActivity(intent)
        }
    }

    fun isDownloading(): Boolean {
        return downloaderTask.isDownloading
    }

    override fun onProgress(progress: Float) {
        this.progress = progress
        this.state = DownloadableItemState.Downloading
        // fireDownloadStateChange()
    }

    override fun downloadStarted(f: File) {
        this.filepath = f.absolutePath
        this.filename = f.name
        this.state = DownloadableItemState.Start
        Log.e(TAG, "start downloading to " + f.absolutePath)
        fireDownloadStateChange()

        startRefreshTimer()
    }

    override fun downloadFinished(f: File) {
        this.filepath = f.absolutePath
        this.filename = f.name
        this.state = DownloadableItemState.End

        stopRefreshTimer()

        Log.e(TAG, "finished downloading to " + f.absolutePath)
        fireDownloadStateChange()
    }

    override fun downloadFailed() {
        this.state = DownloadableItemState.Failed

        stopRefreshTimer()

        fireDownloadStateChange()
        val message = "failed to download $filepath"
        Log.e(TAG, message)
                viewOps?.onParsingError(checkNotNull(downloaderTask.videoFile), message)
    }

    /**
     * Refresh Timer Support
     */

    companion object {
        const val REFRESH_WINDOW : Long = 1000
    }
    private val refreshTimerLock = Object()
    private var refreshTimer : Timer? = null

    private fun startRefreshTimer() {
        synchronized(refreshTimerLock) {
            refreshTimer?.cancel()
            refreshTimer = Timer()
            refreshTimer?.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    this@DownloadableItem.fireDownloadStateChange()
                }
            }, REFRESH_WINDOW, REFRESH_WINDOW)
        }
    }

    private fun stopRefreshTimer() {
        synchronized(refreshTimerLock) {
            refreshTimer?.cancel()
            refreshTimer = null
        }
    }

    /**
     * Downloadable Item Stage Change Support
     */
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