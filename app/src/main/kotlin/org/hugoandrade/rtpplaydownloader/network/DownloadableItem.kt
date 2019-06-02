package org.hugoandrade.rtpplaydownloader.network

import android.util.Log
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTaskBase
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTaskListener
import java.io.File
import java.util.*
import kotlin.collections.HashSet
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils

class DownloadableItem(private val downloaderTask: DownloaderTaskBase,
                       private val viewOps : DownloadManagerViewOps?) :
        DownloaderTaskListener,
        DownloadableItemStateChangeSupport {

    @Suppress("PrivatePropertyName")
    private val TAG : String = javaClass.simpleName

    var filename: String? = null
    var filepath: String? = null
    var progressSize : Long = 0
    var fileSize : Long = 0
    var state: DownloadableItemState = DownloadableItemState.Start
    var progress : Float = 0f

    private val listenerSet : HashSet<DownloadableItemStateChangeListener>  = HashSet()

    fun startDownload(): DownloadableItem {

        object : Thread() {
            override fun run() {

                val downloading : Boolean = downloaderTask.downloadMediaFileAsync(this@DownloadableItem)

                if (!downloading) {
                    this@DownloadableItem.state = DownloadableItemState.Failed

                    stopRefreshTimer()

                    fireDownloadStateChange()

                    downloadFailed("could not find filetype")
                }
                else {
                    this@DownloadableItem.progress = 0.0f
                    this@DownloadableItem.progressSize = 0
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
        if (!downloaderTask.isDownloading && state == DownloadableItemState.End) {
            if (MediaUtils.doesMediaFileExist(this)) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(filepath))
                intent.setDataAndType(Uri.parse(filepath), "video/mp4")
                viewOps?.getApplicationContext()?.startActivity(intent)
            } else {
                Toast.makeText(viewOps?.getActivityContext(), "File not found", Toast.LENGTH_LONG).show()
            }
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

    override fun onProgress(progress: Long, size : Long) {
        this.progressSize = progress
        this.fileSize = size
        this.state = DownloadableItemState.Downloading
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

    override fun downloadFailed(message: String?) {
        this.state = DownloadableItemState.Failed

        stopRefreshTimer()

        fireDownloadStateChange()
        val message = "failed to download $filepath because of $message"
        Log.e(TAG, message)
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