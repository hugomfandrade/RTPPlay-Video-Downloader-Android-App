package org.hugoandrade.rtpplaydownloader.network.persistance

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.AsyncTask
import java.util.ArrayList

abstract class DatabaseModel {

    // Database fields
    private var database: SQLiteDatabase? = null
    private var dbHelper: DatabaseHelper? = null

    fun onInitialize(context: Context) {
        dbHelper = DatabaseHelper(context)
    }

    fun open() {
        database = dbHelper?.writableDatabase
    }

    fun close() {
        dbHelper?.close()
    }

    fun retrieveAllDownloadableEntries() {
        val task = object : AsyncTask<Void, Void, List<DownloadableEntry>>() {

            override fun doInBackground(vararg params: Void): List<DownloadableEntry> {
                val downloadableItems = ArrayList<DownloadableEntry>()

                val cursor = database?.query(DownloadableEntry.Entry.TABLE_NAME, null, null, null, null, null, null)

                if (cursor != null) {
                    cursor.moveToFirst()
                    while (!cursor.isAfterLast) {
                        downloadableItems.add(DownloadableEntryParser.parse(cursor))
                        cursor.moveToNext()
                    }
                    // make sure to close the cursor
                    cursor.close()
                }

                return downloadableItems
            }

            override fun onPostExecute(downloadableEntries: List<DownloadableEntry>) {

                onGetAllDownloadableEntries(downloadableEntries)
            }
        }
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun insertDownloadableEntry(downloadableEntry: DownloadableEntry) {
        val task = object : AsyncTask<Void, Void, DownloadableEntry?>() {

            override fun doInBackground(vararg params: Void): DownloadableEntry? {
                val c = database?.query(DownloadableEntry.Entry.TABLE_NAME, null, null, null, null, null, null)

                if (c != null) {
                    val nItems = c.count
                    c.close()

                    // Create a new map of values, where column names are the keys
                    val values = DownloadableEntryParser.format(downloadableEntry)
                    values.remove(DownloadableEntry.Entry.Cols._ID)

                    // Insert the new row, returning the primary key value of the new row
                    val newRowId = database?.insert(DownloadableEntry.Entry.TABLE_NAME, null, values)

                    val cursor = database?.query(DownloadableEntry.Entry.TABLE_NAME, null,
                            DownloadableEntry.Entry.Cols._ID + " = ?", arrayOf(newRowId.toString()), null, null, null)

                    if (cursor != null) {
                        cursor.moveToFirst()
                        while (!cursor.isAfterLast) {
                            val downloadableEntry = DownloadableEntryParser.parse(cursor)
                            cursor.close()
                            return downloadableEntry
                        }
                        // make sure to close the cursor

                        cursor.close()
                    }
                }

                return null
            }

            override fun onPostExecute(downloadableEntry: DownloadableEntry?) {

                if (downloadableEntry != null) {
                    onInsertDownloadableEntry(downloadableEntry)
                }
            }
        }
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun deleteAllDownloadableEntries(downloadableEntries: List<DownloadableEntry>) {
        val task = object : AsyncTask<Void, Void, List<DownloadableEntry>>() {

            override fun doInBackground(vararg params: Void): List<DownloadableEntry> {
                val deletedDownloadableEntries = ArrayList<DownloadableEntry>()

                for (deletedDownloadableEntry in deletedDownloadableEntries) {
                    val nRowsAffected = database?.delete(
                            DownloadableEntry.Entry.TABLE_NAME,
                            DownloadableEntry.Entry.Cols._ID + " = ?",
                            arrayOf(deletedDownloadableEntry.id))

                    if (nRowsAffected != 0)
                        deletedDownloadableEntries.add(deletedDownloadableEntry)
                }

                return deletedDownloadableEntries
            }

            override fun onPostExecute(downloadableEntries: List<DownloadableEntry>) {
                super.onPostExecute(downloadableEntries)

                onDeleteAllDownloadableEntries(downloadableEntries)
            }
        }
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun deleteAllDownloadableEntries() {

        val task = object : AsyncTask<Void, Void, Boolean>() {

            override fun doInBackground(vararg params: Void): Boolean {

                database?.delete(DownloadableEntry.Entry.TABLE_NAME, null, null)

                return true
            }

            override fun onPostExecute(aBoolean: Boolean?) {
                super.onPostExecute(aBoolean)
                onResetDatabase(aBoolean!!)
            }
        }
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun updateDownloadableEntry(downloadableEntry: DownloadableEntry) {
        val task = object : AsyncTask<Void, Void, DownloadableEntry?>() {

            override fun doInBackground(vararg params: Void): DownloadableEntry? {
                // Create a new map of values, where column names are the keys
                val values = DownloadableEntryParser.format(downloadableEntry)

                val nRowsAffected = database?.update(
                        DownloadableEntry.Entry.TABLE_NAME, values,
                        DownloadableEntry.Entry.Cols._ID + " = ?",
                        arrayOf(downloadableEntry.id))

                return if (nRowsAffected != 0) {
                    downloadableEntry
                } else {
                    // TODO
                    // update by urlString
                    val cursor = database?.query(DownloadableEntry.Entry.TABLE_NAME,
                            null,
                            DownloadableEntry.Entry.Cols.URL + " = ?",
                            arrayOf(downloadableEntry.urlString),
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

                            android.util.Log.e(TAG, "update::" + d.state.toString() + "::" + rowsAffected)

                            return if (rowsAffected == 0) null else downloadableEntry
                        }
                    }
                    null
                }
            }

            override fun onPostExecute(downloadableEntry: DownloadableEntry?) {

                if (downloadableEntry != null) {
                    onUpdateDownloadableEntry(downloadableEntry)
                }
            }
        }
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun deleteDownloadableEntry(downloadableEntry: DownloadableEntry) {
        val task = object : AsyncTask<Void, Void, DownloadableEntry>() {

            override fun doInBackground(vararg params: Void): DownloadableEntry? {
                val nRowsAffected = database?.delete(
                        DownloadableEntry.Entry.TABLE_NAME,
                        DownloadableEntry.Entry.Cols._ID + " = ?",
                        arrayOf(downloadableEntry.id))

                return if (nRowsAffected == 0)
                    null
                else
                    downloadableEntry
            }

            override fun onPostExecute(downloadableEntry: DownloadableEntry) {
                super.onPostExecute(downloadableEntry)

                onDeleteDownloadableEntry(downloadableEntry)
            }
        }
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    open fun onGetAllDownloadableEntries(downloadableEntries: List<DownloadableEntry>) {
        //No-op
    }

    open fun onInsertDownloadableEntry(downloadableEntry: DownloadableEntry) {
        //No-op
    }

    open fun onDeleteAllDownloadableEntries(downloadableEntries: List<DownloadableEntry>) {
        //No-op
    }

    open fun onDeleteDownloadableEntry(downloadableEntry: DownloadableEntry) {
        //No-op
    }

    open fun onUpdateDownloadableEntry(downloadableEntry: DownloadableEntry) {
        //No-op
    }

    open fun onResetDatabase(wasSuccessfullyDeleted: Boolean) {
        //No-op
    }


    /**
     * Helper class that actually creates and manages
     * the provider's underlying data repository.
     */
    private class DatabaseHelper internal constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(CREATE_DB_TABLE_PASSWORD_ENTRY)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS " + DownloadableEntry.Entry.TABLE_NAME)
            onCreate(db)
        }

        companion object {

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

            private val TAG = DatabaseHelper::class.java.simpleName
        }
    }

    companion object {

        private val TAG = DatabaseModel::class.java.simpleName
    }
}