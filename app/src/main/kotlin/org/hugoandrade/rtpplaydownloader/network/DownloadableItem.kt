package org.hugoandrade.rtpplaydownloader.network

import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTask
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingData
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.hugoandrade.rtpplaydownloader.utils.ListenerSet
import java.io.File

@Entity(tableName = "DownloadableItem")
class DownloadableItem(@PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") val id: Int = 0,// url
                       @ColumnInfo(name = "Url") val url: String,
                       @ColumnInfo(name = "MediaUrl") val mediaUrl: String?,// name
                       @ColumnInfo(name = "Thumbnail") val thumbnailUrl: String?,
                       @ColumnInfo(name = "FileName") val filename: String?,// local
                       @ColumnInfo(name = "FilePath") var filepath: String? = null,
                       @ColumnInfo(name = "FileSize") var filesize: Long? = 0,// url
                       @ColumnInfo(name = "Stage") var state: State? = null,
                       @ColumnInfo(name = "IsArchived") var isArchived: Boolean? = false,
                       @ColumnInfo(name = "DownloadTask") var downloadTask: String? = null,
                       @ColumnInfo(name = "DownloadMessage") var downloadMessage: String? = null) :

        DownloaderTask.Listener {

    constructor(parsingData: ParsingData) : this(
            url = parsingData.url ?: null.toString(),
            mediaUrl = parsingData.mediaUrl ?: null.toString(),
            thumbnailUrl = parsingData.thumbnailUrl ?: null.toString(),
            filename = parsingData.filename ?: null.toString()
    )

    companion object {

        const val TAG : String = "DownloadableItem"

        const val DOWNLOAD_SPEED_CALCULATION_TIMESPAN_IN_MILLIS : Long = 1000 // 1second
    }

    @Ignore private val listenerSet : ListenerSet<State.ChangeListener>  = ListenerSet()

    // run time
    @Ignore var downloadingSpeed : Float = 0f // Per Second
    @Ignore var remainingTime : Long = 0 // In Millis
    @Ignore var progress : Float = 0f
    @Ignore var progressSize : Long = 0
    @Ignore private var oldTimestamp = System.currentTimeMillis()
    @Ignore private var oldDownloadSize: Long = 0L

    init {
        this.filesize = filesize?: 0L
        this.state = state ?: State.Start
        this.progress = if (this.state == State.End) 1f else 0f
        this.isArchived = isArchived ?: false
    }

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

    fun addDownloadStateChangeListener(listener: State.ChangeListener) {
        listenerSet.addListener(listener)
    }

    fun removeDownloadStateChangeListener(listener: State.ChangeListener) {
        listenerSet.removeListener(listener)
    }

    internal fun fireDownloadStateChange() {
        listenerSet.lock()
        listenerSet.get().forEach(action = { it.onDownloadStateChange(this@DownloadableItem) })
        listenerSet.release()
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