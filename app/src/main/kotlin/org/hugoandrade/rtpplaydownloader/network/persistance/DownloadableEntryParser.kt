package org.hugoandrade.rtpplaydownloader.network.persistance

import android.content.ContentValues
import android.database.Cursor
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

        fun format(downloadableEntry: DownloadableEntry): ContentValues {
            val values = ContentValues()
            values.put(Entry.Cols._ID, downloadableEntry.id)
            values.put(Entry.Cols.URL, downloadableEntry.urlString)
            values.put(Entry.Cols.FILEPATH, downloadableEntry.filepath)
            values.put(Entry.Cols.FILENAME, downloadableEntry.filename)
            values.put(Entry.Cols.STAGE, downloadableEntry.state?.ordinal)
            values.put(Entry.Cols.PROGRESS, downloadableEntry.progress)
            return values
        }

        fun parse(cursor: Cursor): DownloadableEntry {
            return DownloadableEntry(
                    cursor.getString(cursor.getColumnIndex(Entry.Cols._ID)),
                    cursor.getString(cursor.getColumnIndex(Entry.Cols.URL)),
                    cursor.getString(cursor.getColumnIndex(Entry.Cols.FILEPATH)),
                    cursor.getString(cursor.getColumnIndex(Entry.Cols.FILENAME)),
                    DownloadableItemState.values()[cursor.getInt(cursor.getColumnIndex(Entry.Cols.STAGE))],
                    cursor.getFloat(cursor.getColumnIndex(Entry.Cols.PROGRESS)))
        }
    }
}