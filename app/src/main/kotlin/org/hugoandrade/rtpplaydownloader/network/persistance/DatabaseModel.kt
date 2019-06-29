package org.hugoandrade.rtpplaydownloader.network.persistance

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.hugoandrade.rtpplaydownloader.DevConstants
import org.hugoandrade.rtpplaydownloader.network.DownloadableItem
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.Executors

abstract class DatabaseModel {

    companion object {
        private val TAG = DatabaseModel::class.java.simpleName
    }

    private lateinit var mPresenterOps: WeakReference<PersistencePresenterOps>

    private val persistenceExecutors = Executors.newFixedThreadPool(DevConstants.nPersistenceThreads)

    // Database fields
    private var database: SQLiteDatabase? = null
    private var dbHelper: DatabaseHelper? = null

    fun onCreate(presenter: PersistencePresenterOps) {
        mPresenterOps = WeakReference(presenter)

        dbHelper = mPresenterOps.get()?.getActivityContext()?.let { DatabaseHelper(it) }

        database = dbHelper?.writableDatabase
    }

    fun onDestroy() {
        mPresenterOps.clear()

        dbHelper?.close()

        persistenceExecutors.shutdownNow()
    }

    fun retrieveAllDownloadableEntries() {

        persistenceExecutors.execute {

            val downloadableItems = ArrayList<DownloadableEntry>()

            val cursor = database?.query(DownloadableEntry.Entry.TABLE_NAME,
                    null,
                    DownloadableEntry.Entry.Cols.IS_ARCHIVED + " = ?",
                    arrayOf("0"),
                    null,
                    null,
                    null)

            if (cursor != null) {
                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    downloadableItems.add(DownloadableEntryParser.parse(cursor))
                    cursor.moveToNext()
                }
                // make sure to close the cursor
                cursor.close()
            }

            mPresenterOps.get()?.onGetAllDownloadableEntries(downloadableItems)
        }
    }

    fun insertDownloadableEntry(downloadableItem: DownloadableItem) {

        persistenceExecutors.execute {

            val c = database?.query(DownloadableEntry.Entry.TABLE_NAME, null, null, null, null, null, null)
            var d : DownloadableEntry? = null

            if (c != null) {
                c.close()

                // Create a new map of values, where column names are the keys
                val values = DownloadableEntryParser.format(DownloadableEntryParser.parse(downloadableItem))
                values.remove(DownloadableEntry.Entry.Cols._ID)

                // Insert the new row, returning the primary key value of the new row
                val newRowId = database?.insert(DownloadableEntry.Entry.TABLE_NAME, null, values)

                val cursor = database?.query(DownloadableEntry.Entry.TABLE_NAME, null,
                        DownloadableEntry.Entry.Cols._ID + " = ?", arrayOf(newRowId.toString()), null, null, null)

                if (cursor != null) {
                    cursor.moveToFirst()
                    while (!cursor.isAfterLast) {
                        val downloadableEntry = DownloadableEntryParser.parse(cursor)
                        downloadableItem.id = downloadableEntry.id
                        cursor.close()
                        d = downloadableEntry
                        break
                    }
                    // make sure to close the cursor

                    cursor.close()
                }
            }

            d?.let { mPresenterOps.get()?.onInsertDownloadableEntry(it) }
        }
    }

    fun deleteAllDownloadableEntries(downloadableEntries: List<DownloadableEntry>) {

        persistenceExecutors.execute {

            val deletedDownloadableEntries = ArrayList<DownloadableEntry>()

            for (deletedDownloadableEntry in deletedDownloadableEntries) {
                val nRowsAffected = database?.delete(
                        DownloadableEntry.Entry.TABLE_NAME,
                        DownloadableEntry.Entry.Cols._ID + " = ?",
                        arrayOf(deletedDownloadableEntry.id))

                if (nRowsAffected != 0)
                    deletedDownloadableEntries.add(deletedDownloadableEntry)
            }

            mPresenterOps.get()?.onDeleteAllDownloadableEntries(deletedDownloadableEntries)
        }
    }

    fun deleteAllDownloadableEntries() {

        persistenceExecutors.execute {

            database?.delete(DownloadableEntry.Entry.TABLE_NAME, null, null)

            mPresenterOps.get()?.onResetDatabase(true)
        }
    }

    fun updateDownloadableEntry(downloadableItem: DownloadableItem) {

        persistenceExecutors.execute {

            // Create a new map of values, where column names are the keys
            val downloadableEntry: DownloadableEntry = DownloadableEntryParser.parse(downloadableItem)
            val values = DownloadableEntryParser.format(downloadableEntry)
            var ret : DownloadableEntry? = null

            if (downloadableEntry.id == null) {
                ret = downloadableEntry
            }
            else {

                val nRowsAffected = database?.update(
                        DownloadableEntry.Entry.TABLE_NAME, values,
                        DownloadableEntry.Entry.Cols._ID + " = ?",
                        arrayOf(downloadableEntry.id))

                if (nRowsAffected != 0) {
                    ret = downloadableEntry
                } else {
                    val cursor = database?.query(DownloadableEntry.Entry.TABLE_NAME,
                            null,
                            DownloadableEntry.Entry.Cols._ID + " = ?",
                            arrayOf(downloadableEntry.id),
                            null, null, null)

                    if (cursor != null) {
                        cursor.moveToFirst()
                        if (!cursor.isAfterLast) {
                            val d = DownloadableEntryParser.parse(cursor)
                            cursor.close()

                            d.filename = downloadableEntry.filename
                            d.filepath = downloadableEntry.filepath
                            d.urlString = downloadableEntry.urlString
                            d.state = downloadableEntry.state
                            d.isArchived = downloadableEntry.isArchived

                            val rowsAffected = database?.update(
                                    DownloadableEntry.Entry.TABLE_NAME,
                                    DownloadableEntryParser.format(d),
                                    DownloadableEntry.Entry.Cols._ID + " = ?",
                                    arrayOf(d.id))

                            if (rowsAffected != 0) {
                                ret = downloadableEntry
                            }
                        }
                    }
                }
            }

            ret?.let { mPresenterOps.get()?.onUpdateDownloadableEntry(it) }
        }
    }

    fun deleteDownloadableEntry(downloadableEntry: DownloadableEntry) {

        persistenceExecutors.execute {

            val nRowsAffected = database?.delete(
                    DownloadableEntry.Entry.TABLE_NAME,
                    DownloadableEntry.Entry.Cols._ID + " = ?",
                    arrayOf(downloadableEntry.id))

            var ret : DownloadableEntry? = null
            if (nRowsAffected != 0) {
                ret = downloadableEntry
            }

            ret?.let { mPresenterOps.get()?.onDeleteDownloadableEntry(it) }
        }
    }


    /**
     * Helper class that actually creates and manages
     * the provider's underlying data repository.
     */
    private class DatabaseHelper internal constructor(context: Context) :
            SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(CREATE_DB_TABLE_PASSWORD_ENTRY)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS " + DownloadableEntry.Entry.TABLE_NAME)
            onCreate(db)
        }

        companion object {

            private val TAG = DatabaseHelper::class.java.simpleName

            private val DATABASE_NAME = "RTPPlayDownloadAppDB"
            private val DATABASE_VERSION = 1

            private val CREATE_DB_TABLE_PASSWORD_ENTRY = " CREATE TABLE " + DownloadableEntry.Entry.TABLE_NAME + " (" +
                    " " + DownloadableEntry.Entry.Cols._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    " " + DownloadableEntry.Entry.Cols.URL + " TEXT NULL, " +
                    " " + DownloadableEntry.Entry.Cols.FILENAME + " TEXT NULL, " +
                    " " + DownloadableEntry.Entry.Cols.FILEPATH + " TEXT NULL, " +
                    " " + DownloadableEntry.Entry.Cols.STAGE + " TEXT NULL, " +
                    " " + DownloadableEntry.Entry.Cols.IS_ARCHIVED + " INTEGER NULL " +
                    " );"
        }
    }
}