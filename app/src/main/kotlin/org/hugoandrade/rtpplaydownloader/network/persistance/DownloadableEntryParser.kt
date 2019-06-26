package org.hugoandrade.rtpplaydownloader.network.persistance

import android.content.ContentValues
import android.database.Cursor
import org.hugoandrade.rtpplaydownloader.network.DownloadableItem
import org.hugoandrade.rtpplaydownloader.network.DownloadableItemState
import org.hugoandrade.rtpplaydownloader.network.persistance.DownloadableEntry.*

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
                    cursor.getString(cursor.getColumnIndex(Entry.Cols.FILEPATH)),
                    cursor.getString(cursor.getColumnIndex(Entry.Cols.FILENAME)),
                    DownloadableItemState.values()[cursor.getInt(cursor.getColumnIndex(Entry.Cols.STAGE))],
                    cursor.getInt(cursor.getColumnIndex(Entry.Cols.IS_ARCHIVED)) == 1)
        }

        fun parse(downloadableItem: DownloadableItem): DownloadableEntry {
            return DownloadableEntry(
                    // TODO
                    downloadableItem.id,
                    downloadableItem.url,
                    downloadableItem.filename,
                    downloadableItem.filename,
                    downloadableItem.state,
                    false)
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
                    downloadableEntry.urlString,
                    downloadableEntry.filename,
                    downloadableEntry.filename,
                    downloadableEntry.state,
                    downloadableEntry.isArchived)
        }

        fun format(downloadableEntry: DownloadableEntry): ContentValues {
            val values = ContentValues()
            values.put(Entry.Cols._ID, downloadableEntry.id)
            values.put(Entry.Cols.URL, downloadableEntry.urlString)
            values.put(Entry.Cols.FILEPATH, downloadableEntry.filepath)
            values.put(Entry.Cols.FILENAME, downloadableEntry.filename)
            values.put(Entry.Cols.STAGE, downloadableEntry.state?.ordinal)
            values.put(Entry.Cols.IS_ARCHIVED, downloadableEntry.isArchived)
            return values
        }
    }
}