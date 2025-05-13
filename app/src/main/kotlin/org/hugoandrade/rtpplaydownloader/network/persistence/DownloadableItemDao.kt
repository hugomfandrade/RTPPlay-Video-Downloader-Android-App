package org.hugoandrade.rtpplaydownloader.network.persistence

import androidx.room.*
import org.hugoandrade.rtpplaydownloader.network.AndroidDownloadableItem

@Dao
interface DownloadableItemDao {

    @Query("SELECT * from DownloadableItem where _id = :id LIMIT 1")
    fun getItemById(id: Long): AndroidDownloadableItem?

    @Query("SELECT * FROM DownloadableItem WHERE IsArchived = 0")
    fun getItems(): List<AndroidDownloadableItem>

    @Query("SELECT * FROM DownloadableItem WHERE IsArchived = 1")
    fun getArchivedItems(): List<AndroidDownloadableItem>

    @Query("SELECT * FROM DownloadableItem")
    fun getAllItems(): List<AndroidDownloadableItem>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertItem(downloadableItem: AndroidDownloadableItem): Long

    @Delete
    fun insertItems(vararg downloadableItem: AndroidDownloadableItem)

    @Query("DELETE FROM DownloadableItem")
    fun deleteAllItems()

    @Delete
    fun deleteItems(vararg downloadableItem: AndroidDownloadableItem)

    @Delete
    fun deleteItem(downloadableItem: AndroidDownloadableItem) : Int

    @Update
    fun updateItems(vararg downloadableItem: AndroidDownloadableItem)

    @Update
    fun updateItem(downloadableItem: AndroidDownloadableItem): Int
}