package org.hugoandrade.rtpplaydownloader.network.persistance

import org.hugoandrade.rtpplaydownloader.network.DownloadableItemState

class DownloadableEntry {

    object Entry {

        val TABLE_NAME = "DownloadableItemEntry"

        object Cols {
            val _ID = "_id"
            val URL = "Url"
            val FILENAME = "FileName"
            val FILEPATH = "FilePath"
            val FILESIZE = "FileSize"
            val THUMBNAIL_URL = "Thumbnail"
            val STAGE = "Stage"
            val IS_ARCHIVED = "IsArchived"
        }
    }

    var id: String? = null
    var url: String
    var mediaFileName: String
    var mediaUrl: String? = null
    var filesize: Long? = null
    var thumbnailUrl: String? = null
    var state: DownloadableItemState? = null
    var isArchived: Boolean? = null

    constructor(id: String?,
                url: String,
                mediaFileName: String,
                filepath: String?,
                filesize: Long?,
                thumbnailUrl: String?,
                state: DownloadableItemState?,
                isArchived: Boolean?) {
        this.id = id
        this.url = url
        this.mediaFileName = mediaFileName
        this.mediaUrl = filepath
        this.filesize = filesize
        this.thumbnailUrl = thumbnailUrl
        this.state = state
        this.isArchived = isArchived
    }
}