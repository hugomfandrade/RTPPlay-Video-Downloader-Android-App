package org.hugoandrade.rtpplaydownloader.network.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.hugoandrade.rtpplaydownloader.network.DownloadableItem

@Database(entities = [DownloadableItem::class], version = 4)
@TypeConverters(DownloadableItemStateConverter::class)
abstract class DownloadableItemDatabase : RoomDatabase() {

    abstract fun downloadableItemDao(): DownloadableItemDao

    companion object {

        private val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS DownloadableItem_new (_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, Url TEXT NOT NULL, MediaUrl TEXT, Thumbnail TEXT, FileName TEXT, FilePath TEXT, FileSize INTEGER, Stage INTEGER, IsArchived INTEGER, DownloadTask TEXT, DownloadMessage TEXT)");
                database.execSQL("INSERT INTO DownloadableItem_new (_id,Url,MediaUrl,Thumbnail,FileName,FilePath,FileSize,Stage,IsArchived,DownloadTask,DownloadMessage) SELECT _id,Url,MediaUrl,Thumbnail,FileName,FilePath,FileSize,Stage,IsArchived,DownloadTask,DownloadMessage FROM DownloadableItem")
                database.execSQL("DROP TABLE DownloadableItem")
                database.execSQL("ALTER TABLE DownloadableItem_new RENAME TO DownloadableItem");
            }
        }

        @Volatile
        private var INSTANCE: DownloadableItemDatabase? = null

        fun getDatabase(context: Context): DownloadableItemDatabase {
            val instance = INSTANCE
            if (instance == null) {
                synchronized(DownloadableItemDatabase::class.java) {
                    val instance = INSTANCE
                    if (instance == null) {
                        val newInstance = Room.databaseBuilder(context,
                                DownloadableItemDatabase::class.java, "RTPPlayDownloadAppDB")
                                .addMigrations(MIGRATION_3_4)
                                .build()
                        INSTANCE = newInstance
                        return newInstance
                    }
                    return instance
                }
            }
            return instance
        }
    }
}