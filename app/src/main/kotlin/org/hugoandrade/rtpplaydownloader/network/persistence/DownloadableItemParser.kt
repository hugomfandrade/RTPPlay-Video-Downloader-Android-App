package org.hugoandrade.rtpplaydownloader.network.persistence

import android.content.ContentValues
import android.database.Cursor
import org.hugoandrade.rtpplaydownloader.network.DownloadableItem
import org.hugoandrade.rtpplaydownloader.network.DownloadableItem.Entry

@Deprecated(message = "user room instead")
class DownloadableItemParser

/**
 * Ensure this class is only used as a utility.
 */
private constructor() {

    init {
        throw AssertionError()
    }

    companion object {

        fun parse(cursor: Cursor): DownloadableItem {
            return DownloadableItem(
                    cursor.getInt(cursor.getColumnIndex(Entry.Cols._ID)),
                    cursor.getString(cursor.getColumnIndex(Entry.Cols.URL)),
                    cursor.getString(cursor.getColumnIndex(Entry.Cols.MEDIA_URL)),
                    cursor.getString(cursor.getColumnIndex(Entry.Cols.THUMBNAIL_URL)),
                    cursor.getString(cursor.getColumnIndex(Entry.Cols.FILENAME)),
                    cursor.getString(cursor.getColumnIndex(Entry.Cols.FILEPATH)),
                    cursor.getLong(cursor.getColumnIndex(Entry.Cols.FILESIZE)),
                    DownloadableItem.State.values()[cursor.getInt(cursor.getColumnIndex(Entry.Cols.STAGE))],
                    cursor.getInt(cursor.getColumnIndex(Entry.Cols.IS_ARCHIVED)) == 1,
                    cursor.getString(cursor.getColumnIndex(Entry.Cols.DOWNLOAD_TASK)),
                    cursor.getString(cursor.getColumnIndex(Entry.Cols.DOWNLOAD_MESSAGE)))
        }

        fun format(downloadableItem: DownloadableItem): ContentValues {
            val values = ContentValues()
            values.put(Entry.Cols._ID, downloadableItem.id)
            values.put(Entry.Cols.URL, downloadableItem.url)
            values.put(Entry.Cols.MEDIA_URL, downloadableItem.mediaUrl)
            values.put(Entry.Cols.THUMBNAIL_URL, downloadableItem.thumbnailUrl)
            values.put(Entry.Cols.FILEPATH, downloadableItem.filepath)
            values.put(Entry.Cols.FILENAME, downloadableItem.filename)
            values.put(Entry.Cols.FILESIZE, downloadableItem.filesize)
            values.put(Entry.Cols.STAGE, downloadableItem.state?.ordinal)
            values.put(Entry.Cols.IS_ARCHIVED, downloadableItem.isArchived)
            values.put(Entry.Cols.DOWNLOAD_TASK, downloadableItem.downloadTask)
            values.put(Entry.Cols.DOWNLOAD_MESSAGE, downloadableItem.downloadMessage)
            return values
        }
    }
}