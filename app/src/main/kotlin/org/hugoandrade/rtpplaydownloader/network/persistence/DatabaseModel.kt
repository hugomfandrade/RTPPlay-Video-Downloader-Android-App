package org.hugoandrade.rtpplaydownloader.network.persistence

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.hugoandrade.rtpplaydownloader.DevConstants
import org.hugoandrade.rtpplaydownloader.network.DownloadableItem
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.Executors

abstract class DatabaseModel(private val application: Application) {

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

        dbHelper = DatabaseHelper(application)

        database = dbHelper?.writableDatabase
    }

    fun onDestroy() {
        mPresenterOps.clear()

        dbHelper?.close()

        persistenceExecutors.shutdownNow()
    }

    fun retrieveAllDownloadableItems() {

        if (persistenceExecutors.isShutdown || persistenceExecutors.isTerminated) return
        persistenceExecutors.execute {

            mPresenterOps.get()?.onDownloadableItemsRetrieved(retrieveDownloadableItems(false))
        }
    }

    fun retrieveArchivedDownloadableItems() {

        if (persistenceExecutors.isShutdown || persistenceExecutors.isTerminated) return
        persistenceExecutors.execute {

            mPresenterOps.get()?.onArchivedDownloadableItemsRetrieved(retrieveDownloadableItems(true))
        }
    }


    private fun retrieveDownloadableItems(isArchived: Boolean) : ArrayList<DownloadableItem> {

            val downloadableItems = ArrayList<DownloadableItem>()

            val cursor = database?.query(DownloadableItem.Entry.TABLE_NAME,
                    null,
                    DownloadableItem.Entry.Cols.IS_ARCHIVED + " = ?",
                    arrayOf(if (isArchived) "1" else "0"),
                    null,
                    null,
                    null)

            if (cursor != null) {
                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    downloadableItems.add(DownloadableItemParser.parse(cursor))
                    cursor.moveToNext()
                }
                // make sure to close the cursor
                cursor.close()
            }

        return downloadableItems
    }

    fun insertDownloadableItem(downloadableItem: DownloadableItem) {

        if (persistenceExecutors.isShutdown || persistenceExecutors.isTerminated) return
        persistenceExecutors.execute {

            var d : DownloadableItem? = null

            // Create a new map of values, where column names are the keys
            val values = DownloadableItemParser.format(downloadableItem)
            values.remove(DownloadableItem.Entry.Cols._ID)

            // Insert the new row, returning the primary key value of the new row
            val newRowId = database?.insert(DownloadableItem.Entry.TABLE_NAME, null, values)

            val cursor = database?.query(DownloadableItem.Entry.TABLE_NAME, null,
                    DownloadableItem.Entry.Cols._ID + " = ?", arrayOf(newRowId.toString()), null, null, null)

            if (cursor != null) {
                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    val downloadableEntry = DownloadableItemParser.parse(cursor)
                    downloadableItem.id = downloadableEntry.id
                    cursor.close()
                    d = downloadableEntry
                    break
                }

                // make sure to close the cursor
                cursor.close()
            }

            mPresenterOps.get()?.onDownloadableItemInserted(d)
        }
    }

    fun deleteDownloadableItems(downloadableItems: List<DownloadableItem>) {

        if (persistenceExecutors.isShutdown || persistenceExecutors.isTerminated) return
        persistenceExecutors.execute {

            val deletedDownloadableEntries = ArrayList<DownloadableItem>()

            for (deletedDownloadableEntry in downloadableItems) {
                val nRowsAffected = database?.delete(
                        DownloadableItem.Entry.TABLE_NAME,
                        DownloadableItem.Entry.Cols._ID + " = ?",
                        arrayOf(deletedDownloadableEntry.id.toString()))

                if (nRowsAffected != 0)
                    deletedDownloadableEntries.add(deletedDownloadableEntry)
            }

            mPresenterOps.get()?.onDownloadableItemsDeleted(deletedDownloadableEntries)
        }
    }

    fun deleteAllDownloadableItem() {

        if (persistenceExecutors.isShutdown || persistenceExecutors.isTerminated) return
        persistenceExecutors.execute {

            database?.delete(DownloadableItem.Entry.TABLE_NAME, null, null)

            mPresenterOps.get()?.onDatabaseReset(true)
        }
    }

    fun updateDownloadableEntry(downloadableItem: DownloadableItem) {

        if (persistenceExecutors.isShutdown || persistenceExecutors.isTerminated) return
        persistenceExecutors.execute {

            var ret : DownloadableItem? = null

            if (downloadableItem.id != -1) {
                val ids : Array<String> = arrayOf(downloadableItem.id.toString())

                val rowsAffected = database?.update(
                        DownloadableItem.Entry.TABLE_NAME,
                        DownloadableItemParser.format(downloadableItem),
                        DownloadableItem.Entry.Cols._ID + " = ?", ids)

                if (rowsAffected != 0) {
                    ret = downloadableItem
                }
            }

            mPresenterOps.get()?.onDownloadableItemUpdated(ret)
        }
    }

    fun deleteDownloadableItem(downloadableItem: DownloadableItem) {

        if (persistenceExecutors.isShutdown || persistenceExecutors.isTerminated) return
        persistenceExecutors.execute {

            val nRowsAffected = database?.delete(
                    DownloadableItem.Entry.TABLE_NAME,
                    DownloadableItem.Entry.Cols._ID + " = ?",
                    arrayOf(downloadableItem.id.toString()))

            var ret : DownloadableItem? = null
            if (nRowsAffected != 0) {
                ret = downloadableItem
            }

            mPresenterOps.get()?.onDownloadableItemDeleted(ret)
        }
    }


    /**
     * Helper class that actually creates and manages
     * the provider's underlying data repository.
     */
    private class DatabaseHelper internal constructor(context: Context) :
            SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(CREATE_DB_TABLE_DOWNLOADABLE_ENTRY)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS " + DownloadableItem.Entry.TABLE_NAME)
            onCreate(db)
        }

        override fun onDowngrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
            if (db != null) {
                db.execSQL("DROP TABLE IF EXISTS " + DownloadableItem.Entry.TABLE_NAME)
                onCreate(db)
            }
        }

        companion object {

            private const val DATABASE_NAME = "RTPPlayDownloadAppDB"
            private const val DATABASE_VERSION = 3

            private val CREATE_DB_TABLE_DOWNLOADABLE_ENTRY = " CREATE TABLE " + DownloadableItem.Entry.TABLE_NAME + " (" +
                    " " + DownloadableItem.Entry.Cols._ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + ", " +
                    " " + DownloadableItem.Entry.Cols.URL + " TEXT NULL" + ", " +
                    " " + DownloadableItem.Entry.Cols.MEDIA_URL + " TEXT NULL" + ", " +
                    " " + DownloadableItem.Entry.Cols.THUMBNAIL_URL + " TEXT NULL" + ", " +
                    " " + DownloadableItem.Entry.Cols.FILENAME + " TEXT NULL" + ", " +
                    " " + DownloadableItem.Entry.Cols.FILEPATH + " TEXT NULL" + ", " +
                    " " + DownloadableItem.Entry.Cols.FILESIZE + " TEXT NULL" + ", " +
                    " " + DownloadableItem.Entry.Cols.STAGE + " TEXT NULL" + ", " +
                    " " + DownloadableItem.Entry.Cols.IS_ARCHIVED + " INTEGER NULL" + ", " +
                    " " + DownloadableItem.Entry.Cols.DOWNLOAD_MESSAGE + " TEXT NULL" + ", " +
                    " " + DownloadableItem.Entry.Cols.DOWNLOAD_TASK + " TEXT NULL" +
                    " );"
        }
    }
}