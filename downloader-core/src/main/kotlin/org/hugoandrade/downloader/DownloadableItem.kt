package org.hugoandrade.downloader

import org.hugoandrade.downloader.download.DownloaderTask
import org.hugoandrade.downloader.parsing.ParsingData
import org.hugoandrade.downloader.utils.ListenerSet
import org.hugoandrade.downloader.utils.MediaUtils
import java.io.File

open class DownloadableItem(open val id: Int = 0,// url
                            open val url: String,
                            open val mediaUrl: String?,// name
                            open val thumbnailUrl: String?,
                            open val filename: String?,// local
                            open var filepath: String? = null,
                            open var filesize: Long? = 0,// url
                            open var state: State? = null,
                            open var isArchived: Boolean? = false,
                            open var downloadTask: String? = null,
                            open var downloadMessage: String? = null) :

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

    open val listenerSet : ListenerSet<State.ChangeListener> = ListenerSet()

    // run time
    open var downloadingSpeed : Float = 0f // Per Second
    open var remainingTime : Long = 0 // In Millis
    open var progress : Float = 0f
    open var progressSize : Long = 0

    open var oldTimestamp = System.currentTimeMillis()
    open var oldDownloadSize: Long = 0L

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

        fireDownloadStateChange()
    }

    override fun downloadFinished(f: File) {
        this.filepath = f.absolutePath
        this.state = State.End
        this.downloadMessage = null

        fireDownloadStateChange()
    }

    override fun downloadFailed(message: String?) {
        this.state = State.Failed
        this.downloadMessage = message

        fireDownloadStateChange()
        val message = "failed to download $filepath because of $message"
    }

    fun addDownloadStateChangeListener(listener: State.ChangeListener) {
        listenerSet.addListener(listener)
    }

    fun removeDownloadStateChangeListener(listener: State.ChangeListener) {
        listenerSet.removeListener(listener)
    }

    fun fireDownloadStateChange() {
        listenerSet.lock()
        listenerSet.get().forEach(action = { it.onDownloadStateChange(this@DownloadableItem) })
        listenerSet.release()
    }

    enum class State {
        Start,
        Downloading,
        Paused,
        End,

        Failed;

        companion object {
            fun isOver(state : State?) : Boolean {
                return state == End || state == Failed
            }
        }

        interface ChangeListener {
            fun onDownloadStateChange(downloadableItem: DownloadableItem)
        }
    }
}