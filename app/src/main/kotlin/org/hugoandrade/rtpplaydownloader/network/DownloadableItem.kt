package org.hugoandrade.rtpplaydownloader.network

import android.util.Log
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTask
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
                       var state: State?,
                       var isArchived: Boolean?,
                       var downloadTask: String?,
                       var downloadMessage: String?) :

        DownloaderTask.Listener {

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
            this(-1, url, mediaUrl, thumbnailUrl, filename, null, 0, null, false, null, null)


    override fun onProgress(downloadedSize: Long, totalSize : Long) {
        this.state = State.Downloading

        this.progressSize = downloadedSize
        this.filesize = totalSize

        this.progress = downloadedSize.toFloat() / totalSize.toFloat()

        val tmpTimestamp: Long = System.currentTimeMillis()
        if (updateProgressUtils()) {
            oldTimestamp = tmpTimestamp
            oldDownloadSize = downloadedSize
        }

        // fireDownloadStateChange()
    }

    fun updateProgressUtils(): Boolean {

        val tmpTimestamp: Long = System.currentTimeMillis()
        if ((tmpTimestamp - oldTimestamp) >= DOWNLOAD_SPEED_CALCULATION_TIMESPAN_IN_MILLIS) {
            val downloadingSpeedPerSecond : Float = MediaUtils.calculateDownloadingSpeed(oldTimestamp, tmpTimestamp, oldDownloadSize, progressSize)
            val remainingTimeInMillis: Long = MediaUtils.calculateRemainingDownloadTime(oldTimestamp, tmpTimestamp, oldDownloadSize, progressSize, filesize?:0)

            this.downloadingSpeed = downloadingSpeedPerSecond
            this.remainingTime = remainingTimeInMillis
            return true
        }
        return false
    }

    override fun downloadStarted(f: File) {
        this.filepath = f.absolutePath
        this.state = State.Start
        this.downloadMessage = null

        this.progressSize = 0L
        this.oldTimestamp = System.currentTimeMillis()
        this.oldDownloadSize = progressSize

        Log.e(TAG, "start downloading to " + f.absolutePath)
        fireDownloadStateChange()
    }

    override fun downloadFinished(f: File) {
        this.filepath = f.absolutePath
        this.state = State.End
        this.downloadMessage = null

        Log.e(TAG, "finished downloading to " + f.absolutePath)
        fireDownloadStateChange()
    }

    override fun downloadFailed(message: String?) {
        this.state = State.Failed
        this.downloadMessage = message

        fireDownloadStateChange()
        val message = "failed to download $filepath because of $message"
        Log.e(TAG, message)
    }

    private val listenerSet : ListenerSet<DownloadableItem.State.ChangeListener>  = ListenerSet()

    fun addDownloadStateChangeListener(listener: DownloadableItem.State.ChangeListener) {
        listenerSet.addListener(listener)
    }

    fun removeDownloadStateChangeListener(listener: DownloadableItem.State.ChangeListener) {
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
        this.state = state ?: State.Start
        this.progress = if (this.state == State.End) 1f else 0f
        this.isArchived = isArchived ?: false
    }


    object Entry {

        const val TABLE_NAME = "DownloadableItem"

        object Cols {
            const val _ID = "_id"
            const val URL = "Url"
            const val MEDIA_URL = "MediaUrl"
            const val THUMBNAIL_URL = "Thumbnail"
            const val FILENAME = "FileName"
            const val FILEPATH = "FilePath"
            const val FILESIZE = "FileSize"
            const val STAGE = "Stage"
            const val IS_ARCHIVED = "IsArchived"
            const val DOWNLOAD_MESSAGE = "DownloadMessage"
            const val DOWNLOAD_TASK = "DownloadTask"
        }
    }

    enum class State {
        Start,
        Downloading,
        End,

        Failed;

        interface ChangeListener {
            fun onDownloadStateChange(downloadableItem: DownloadableItem)
        }
    }
}