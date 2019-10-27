package org.hugoandrade.rtpplaydownloader.network

import android.util.Log
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTaskListener
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import java.io.File

class DownloadableItem :
        DownloaderTaskListener,
        DownloadableItemStateChangeSupport {

    @Suppress("PrivatePropertyName")
    private val TAG : String = javaClass.simpleName

    var id: String? = null
    var url: String? = null // url
    var filename: String? // name
    var thumbnailPath: String? // url
    var state: DownloadableItemState = DownloadableItemState.Start
    var isArchived : Boolean = false

    var filepath: String? = null // local
    var fileSize : Long = 0
    var progress : Float = 0f
    var progressSize : Long = 0

    // run time
    var downloadingSpeed : Float = 0f // Per Second
    var remainingTime : Long = 0 // In Millis

    constructor(id: String?,
                url: String?,
                filename: String?,
                filepath: String?,
                filesize: Long?,
                thumbnailPath: String?,
                state: DownloadableItemState?,
                isArchived: Boolean?) {

        this.id = id
        this.url = url
        this.filename = filename
        this.filepath = filepath
        this.fileSize = filesize?: 0L
        this.thumbnailPath = thumbnailPath
        this.state = state ?: DownloadableItemState.Start
        this.progress = if (this.state == DownloadableItemState.End) 1f else 0f
        this.isArchived = isArchived ?: false
    }

    constructor(url: String?,
                filename: String?,
                thumbnailPath: String?) :
        this(null, url, filename, null, 0, thumbnailPath, null, false)

    companion object {
        const val DOWNLOAD_SPEED_CALCULATION_TIMESPAN_IN_MILLIS : Long = 1000 // 1second
    }

    private val listenerSet : HashSet<DownloadableItemStateChangeListener>  = HashSet()

    private var oldTimestamp = System.currentTimeMillis()
    private var oldDownloadSize: Long = 0L

    override fun onProgress(downloadedSize: Long, totalSize : Long) {
        this.state = DownloadableItemState.Downloading

        this.progressSize = downloadedSize
        this.fileSize = totalSize

        this.progress = downloadedSize.toFloat() / totalSize.toFloat()

        val tmpTimestamp: Long = System.currentTimeMillis()
        if ((tmpTimestamp - oldTimestamp) >= DOWNLOAD_SPEED_CALCULATION_TIMESPAN_IN_MILLIS) {
            val downloadingSpeedPerSecond : Float = MediaUtils.calculateDownloadingSpeed(oldTimestamp, tmpTimestamp, oldDownloadSize, downloadedSize)
            val remainingTimeInMillis: Long = MediaUtils.calculateRemainingDownloadTime(oldTimestamp, tmpTimestamp, oldDownloadSize, downloadedSize, totalSize)

            this.downloadingSpeed = downloadingSpeedPerSecond
            this.remainingTime = remainingTimeInMillis

            oldTimestamp = tmpTimestamp
            oldDownloadSize = downloadedSize
        }

        // fireDownloadStateChange()
    }

    override fun downloadStarted(f: File) {
        this.filepath = f.absolutePath
        this.filename = f.name
        this.state = DownloadableItemState.Start

        this.progressSize = 0L
        this.oldTimestamp = System.currentTimeMillis()
        this.oldDownloadSize = progressSize

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

    override fun downloadFailed(message: String?) {
        this.state = DownloadableItemState.Failed

        fireDownloadStateChange()
        val message = "failed to download $filepath because of $message"
        Log.e(TAG, message)
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

    internal fun fireDownloadStateChange() {
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