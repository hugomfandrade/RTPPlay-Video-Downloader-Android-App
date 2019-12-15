package org.hugoandrade.rtpplaydownloader.network

import android.util.Log
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTaskListener
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.hugoandrade.rtpplaydownloader.utils.ListenerSet
import java.io.File

class DownloadableItem(var id: Int,// url
                       val url: String,
                       val mediaUrl: String?,// name
                       val thumbnailUrl: String?,
                       val filename: String?,// local
                       var filepath: String?,
                       var filesize: Long?,// url
                       var state: DownloadableItemState?,
                       var isArchived: Boolean?) :

        DownloaderTaskListener {

    @Suppress("PrivatePropertyName")
    private val TAG : String = javaClass.simpleName

    companion object {
        const val DOWNLOAD_SPEED_CALCULATION_TIMESPAN_IN_MILLIS : Long = 1000 // 1second
    }

    // run time
    var downloadingSpeed : Float = 0f // Per Second
    var remainingTime : Long = 0 // In Millis
    var progress : Float = 0f
    var progressSize : Long = 0
    private var oldTimestamp = System.currentTimeMillis()
    private var oldDownloadSize: Long = 0L

    constructor(url: String,
                mediaUrl: String,
                thumbnailUrl: String?,
                filename: String) :
            this(-1, url, mediaUrl, thumbnailUrl, filename, null, 0, null, false)


    override fun onProgress(downloadedSize: Long, totalSize : Long) {
        this.state = DownloadableItemState.Downloading

        this.progressSize = downloadedSize
        this.filesize = totalSize

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
        this.state = DownloadableItemState.Start

        this.progressSize = 0L
        this.oldTimestamp = System.currentTimeMillis()
        this.oldDownloadSize = progressSize

        Log.e(TAG, "start downloading to " + f.absolutePath)
        fireDownloadStateChange()
    }

    override fun downloadFinished(f: File) {
        this.filepath = f.absolutePath
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

    private val listenerSet : ListenerSet<DownloadableItemState.ChangeListener>  = ListenerSet()

    fun addDownloadStateChangeListener(listener: DownloadableItemState.ChangeListener) {
        listenerSet.addListener(listener)
    }

    fun removeDownloadStateChangeListener(listener: DownloadableItemState.ChangeListener) {
        listenerSet.removeListener(listener)
    }

    internal fun fireDownloadStateChange() {
        while (listenerSet.isLocked){}
        listenerSet.lock()
        listenerSet.get().forEach(action = { it.onDownloadStateChange(this@DownloadableItem) })
        listenerSet.release()
    }

    init {
        this.filesize = filesize?: 0L
        this.state = state ?: DownloadableItemState.Start
        this.progress = if (this.state == DownloadableItemState.End) 1f else 0f
        this.isArchived = isArchived ?: false
    }


    object Entry {

        val TABLE_NAME = "DownloadableItem"

        object Cols {
            val _ID = "_id"
            val URL = "Url"
            val MEDIA_URL = "MediaUrl"
            val THUMBNAIL_URL = "Thumbnail"
            val FILENAME = "FileName"
            val FILEPATH = "FilePath"
            val FILESIZE = "FileSize"
            val STAGE = "Stage"
            val IS_ARCHIVED = "IsArchived"
            val DOWNLOAD_MESSAGE = "DownloadMessage"
            val DOWNLOAD_TASK = "DownloadTask"
        }
    }
}