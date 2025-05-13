package org.hugoandrade.rtpplaydownloader.network

import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import org.hugoandrade.downloader.DownloadableItem
import org.hugoandrade.downloader.parsing.ParsingData
import org.hugoandrade.downloader.utils.ListenerSet
import java.io.File

@Entity(tableName = "DownloadableItem")
class AndroidDownloadableItem(@PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") override val id: Int = 0,// url
                              @ColumnInfo(name = "Url") override val url: String,
                              @ColumnInfo(name = "MediaUrl") override val mediaUrl: String?,// name
                              @ColumnInfo(name = "Thumbnail") override val thumbnailUrl: String?,
                              @ColumnInfo(name = "FileName") override val filename: String?,// local
                              @ColumnInfo(name = "FilePath") override var filepath: String? = null,
                              @ColumnInfo(name = "FileSize") override var filesize: Long? = 0,// url
                              @ColumnInfo(name = "Stage") override var state: State? = null,
                              @ColumnInfo(name = "IsArchived") override var isArchived: Boolean? = false,
                              @ColumnInfo(name = "DownloadTask") override var downloadTask: String? = null,
                              @ColumnInfo(name = "DownloadMessage") override var downloadMessage: String? = null) :

        DownloadableItem(id, url, mediaUrl, thumbnailUrl, filename, filepath, filesize, state, isArchived, downloadTask, downloadMessage) {

    constructor(parsingData: ParsingData) : this(
            url = parsingData.url ?: null.toString(),
            mediaUrl = parsingData.mediaUrl ?: null.toString(),
            thumbnailUrl = parsingData.thumbnailUrl ?: null.toString(),
            filename = parsingData.filename ?: null.toString()
    )

    companion object {

        const val TAG : String = "DownloadableItem"
    }

    // run time
    @Ignore override var downloadingSpeed : Float = 0f // Per Second
    @Ignore override var remainingTime : Long = 0 // In Millis
    @Ignore override var progress : Float = 0f
    @Ignore override var progressSize : Long = 0


    @Ignore override val listenerSet : ListenerSet<State.ChangeListener> = ListenerSet()
    @Ignore override var oldTimestamp = System.currentTimeMillis()
    @Ignore override var oldDownloadSize: Long = 0L

    init {
        this.filesize = filesize?: 0L
        this.state = state ?: State.Start
        this.progress = if (this.state == State.End) 1f else 0f
        this.isArchived = isArchived ?: false
    }

    override fun downloadStarted(f: File) {
        Log.e(TAG, "start downloading to " + f.absolutePath)
        super.downloadStarted(f)
    }

    override fun downloadFinished(f: File) {
        Log.e(TAG, "finished downloading to " + f.absolutePath)
        super.downloadFinished(f)
    }

    override fun downloadFailed(message: String?) {
        val message = "failed to download $filepath because of $message"
        Log.e(TAG, message)
        super.downloadFailed(message)
    }
}

fun AndroidDownloadableItem.toBase(): DownloadableItem = DownloadableItem(
    id = id,
    url = url,
    mediaUrl = mediaUrl,
    thumbnailUrl = thumbnailUrl,
    filename = filename,
    filepath = filepath,
    filesize = filesize ?: 0L,
    state = state,
    isArchived = isArchived ?: false,
    downloadTask = downloadTask,
    downloadMessage = downloadMessage
)

fun DownloadableItem.toAndroid(): AndroidDownloadableItem = AndroidDownloadableItem(
    id = id,
    url = url,
    mediaUrl = mediaUrl,
    thumbnailUrl = thumbnailUrl,
    filename = filename,
    filepath = filepath,
    filesize = filesize,
    state = state,
    isArchived = isArchived ?: false,
    downloadTask = downloadTask,
    downloadMessage = downloadMessage
)
