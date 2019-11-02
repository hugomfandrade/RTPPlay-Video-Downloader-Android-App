package org.hugoandrade.rtpplaydownloader.network.persistance

import android.content.ContentValues
import android.database.Cursor
import org.hugoandrade.rtpplaydownloader.network.DownloadableItem
import org.hugoandrade.rtpplaydownloader.network.DownloadableItemState
import org.hugoandrade.rtpplaydownloader.network.persistance.DownloadableEntry.Entry

class DownloadableEntryParser

/**
 * Ensure this class is only used as a utility.
 */
private constructor() {

    init {
        throw AssertionError()
    }


    companion object {

        fun parse(cursor: Cursor): DownloadableEntry {
            return DownloadableEntry(
                    cursor.getString(cursor.getColumnIndex(Entry.Cols._ID)),
                    cursor.getString(cursor.getColumnIndex(Entry.Cols.URL)),
                    cursor.getString(cursor.getColumnIndex(Entry.Cols.FILENAME)),
                    cursor.getString(cursor.getColumnIndex(Entry.Cols.FILEPATH)),
                    cursor.getLong(cursor.getColumnIndex(Entry.Cols.FILESIZE)),
                    cursor.getString(cursor.getColumnIndex(Entry.Cols.THUMBNAIL_URL)),
                    DownloadableItemState.values()[cursor.getInt(cursor.getColumnIndex(Entry.Cols.STAGE))],
                    cursor.getInt(cursor.getColumnIndex(Entry.Cols.IS_ARCHIVED)) == 1)
        }

        fun parse(downloadableItem: DownloadableItem): DownloadableEntry {
            return DownloadableEntry(
                    downloadableItem.id,
                    downloadableItem.url,
                    downloadableItem.mediaFileName,
                    downloadableItem.filepath,
                    downloadableItem.fileSize,
                    downloadableItem.thumbnailUrl,
                    downloadableItem.state,
                    downloadableItem.isArchived)
        }

        fun formatCollection(downloadableEntries: List<DownloadableEntry>): List<DownloadableItem> {

            val downloadableItems = ArrayList<DownloadableItem>()

            downloadableEntries.forEach {
                downloadableEntry -> downloadableItems.add(formatSingleton(downloadableEntry))
            }
            return downloadableItems
        }

        fun formatSingleton(downloadableEntry: DownloadableEntry): DownloadableItem {

            return DownloadableItem(
                    downloadableEntry.id,
                    downloadableEntry.url,
                    downloadableEntry.mediaFileName,
                    downloadableEntry.mediaUrl,
                    downloadableEntry.filesize,
                    downloadableEntry.thumbnailUrl,
                    downloadableEntry.state,
                    downloadableEntry.isArchived)
        }

        fun format(downloadableEntry: DownloadableEntry): ContentValues {
            val values = ContentValues()
            values.put(Entry.Cols._ID, downloadableEntry.id)
            values.put(Entry.Cols.URL, downloadableEntry.url)
            values.put(Entry.Cols.FILEPATH, downloadableEntry.mediaUrl)
            values.put(Entry.Cols.FILENAME, downloadableEntry.mediaFileName)
            values.put(Entry.Cols.FILESIZE, downloadableEntry.filesize)
            values.put(Entry.Cols.THUMBNAIL_URL, downloadableEntry.thumbnailUrl)
            values.put(Entry.Cols.STAGE, downloadableEntry.state?.ordinal)
            values.put(Entry.Cols.IS_ARCHIVED, downloadableEntry.isArchived)
            return values
        }
    }
}