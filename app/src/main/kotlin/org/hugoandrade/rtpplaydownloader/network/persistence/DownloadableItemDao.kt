package org.hugoandrade.rtpplaydownloader.network.persistence

import androidx.room.*
import org.hugoandrade.rtpplaydownloader.network.DownloadableItem

@Dao
interface DownloadableItemDao {

    @Query("SELECT * from DownloadableItem where _id = :id LIMIT 1")
    fun getItemById(id: Long): DownloadableItem?

    @Query("SELECT * FROM DownloadableItem WHERE IsArchived = 0")
    fun getItems(): List<DownloadableItem>

    @Query("SELECT * FROM DownloadableItem WHERE IsArchived = 1")
    fun getArchivedItems(): List<DownloadableItem>

    @Query("SELECT * FROM DownloadableItem")
    fun getAllItems(): List<DownloadableItem>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertItem(downloadableItem: DownloadableItem): Long

    @Delete
    fun insertItems(vararg downloadableItem: DownloadableItem)

    @Query("DELETE FROM DownloadableItem")
    fun deleteAllItems()

    @Delete
    fun deleteItems(vararg downloadableItem: DownloadableItem)

    @Delete
    fun deleteItem(downloadableItem: DownloadableItem) : Int

    @Update
    fun updateItems(vararg downloadableItem: DownloadableItem)

    @Update
    fun updateItem(downloadableItem: DownloadableItem): Int
}