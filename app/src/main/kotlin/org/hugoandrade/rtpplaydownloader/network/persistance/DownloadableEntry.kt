package org.hugoandrade.rtpplaydownloader.network.persistance

import org.hugoandrade.rtpplaydownloader.network.DownloadableItemState

class DownloadableEntry() {

    @Suppress("PrivatePropertyName")
    private val TAG : String = javaClass.simpleName


    object Entry {

        val TABLE_NAME = "DownloadableItemEntry"

        object Cols {
            val _ID = "_id"
            val URL = "Url"
            val FILENAME = "FileName"
            val FILEPATH = "FilePath"
            val STAGE = "Stage"
            val PROGRESS = "Progress"
        }
    }

    var id: String? = null
    var urlString: String? = null
    var filename: String? = null
    var filepath: String? = null
    var state: DownloadableItemState? = null
    var progress : Float? = null

    constructor(id: String?,
                urlString: String?,
                filename: String?,
                filepath: String?,
                state: DownloadableItemState?,
                progress: Float?) : this() {
        this.id = id
        this.urlString = urlString
        this.filename = filename
        this.filepath = filepath
        this.state = state
        this.progress = progress
    }
}