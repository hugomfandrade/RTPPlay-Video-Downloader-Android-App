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
            val THUMBNAIL = "Thumbnail"
            val STAGE = "Stage"
            val IS_ARCHIVED = "IsArchived"
        }
    }

    var id: String? = null
    var urlString: String? = null
    var filename: String? = null
    var filepath: String? = null
    var thumbnail: String? = null
    var state: DownloadableItemState? = null
    var isArchived: Boolean? = null

    constructor(id: String?,
                urlString: String?,
                filename: String?,
                filepath: String?,
                thumbnail: String?,
                state: DownloadableItemState?,
                isArchived: Boolean?) : this() {
        this.id = id
        this.urlString = urlString
        this.filename = filename
        this.filepath = filepath
        this.thumbnail = thumbnail
        this.state = state
        this.isArchived = isArchived
    }
}