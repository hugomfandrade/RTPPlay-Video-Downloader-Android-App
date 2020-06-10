package org.hugoandrade.rtpplaydownloader.network.persistence

import android.app.Application
import org.hugoandrade.rtpplaydownloader.DevConstants
import org.hugoandrade.rtpplaydownloader.network.DownloadableItem
import org.hugoandrade.rtpplaydownloader.utils.ListenableFuture
import java.util.*
import java.util.concurrent.Executors


class DownloadableItemRepository(application: Application) {

    companion object {
        private val TAG = DownloadableItemRepository::class.java.simpleName
    }

    private val mItemDao: DownloadableItemDao

    init {
        val db = DownloadableItemDatabase.getDatabase(application)
        mItemDao = db.downloadableItemDao();
    }

    private val persistenceExecutors = Executors.newFixedThreadPool(DevConstants.nPersistenceThreads)


    fun retrieveAllDownloadableItems() : ListenableFuture<List<DownloadableItem>> {
        val future = ListenableFuture<List<DownloadableItem>>()

        if (persistenceExecutors.isShutdown || persistenceExecutors.isTerminated) {
            future.failed("ExecutorService is cancelled")
            return future
        }

        persistenceExecutors.execute {
            future.success(mItemDao.getItems())
        }

        return future
    }

    fun retrieveArchivedDownloadableItems() : ListenableFuture<List<DownloadableItem>> {
        val future = ListenableFuture<List<DownloadableItem>>()

        if (persistenceExecutors.isShutdown || persistenceExecutors.isTerminated) {
            future.failed("ExecutorService is cancelled")
            return future
        }

        persistenceExecutors.execute {
            future.success(mItemDao.getArchivedItems())
        }

        return future
    }

    fun insertDownloadableItem(downloadableItem: DownloadableItem) : ListenableFuture<DownloadableItem> {
        val future = ListenableFuture<DownloadableItem>()

        if (persistenceExecutors.isShutdown || persistenceExecutors.isTerminated) {
            future.failed("ExecutorService is cancelled")
            return future
        }

        persistenceExecutors.execute {
            val id = mItemDao.insertItem(downloadableItem)

            val insertedItem = mItemDao.getItemById(id);

            if (insertedItem == null) {
                future.failed("Failed to insert item")
            }
            else {
                future.success(insertedItem)
            }
        }

        return future
    }

    fun updateDownloadableEntry(downloadableItem: DownloadableItem) : ListenableFuture<DownloadableItem> {
        val future = ListenableFuture<DownloadableItem>()

        if (persistenceExecutors.isShutdown || persistenceExecutors.isTerminated) {
            future.failed("ExecutorService is cancelled")
            return future
        }

        persistenceExecutors.execute {

            val rowsAffected = mItemDao.updateItem(downloadableItem)

            if (rowsAffected != 0) {
                future.success(downloadableItem)
            }
            else {
                future.failed("Failed to update item")
            }
        }

        return future
    }

    fun deleteAllDownloadableItem() : ListenableFuture<Boolean> {
        val future = ListenableFuture<Boolean>()

        if (persistenceExecutors.isShutdown || persistenceExecutors.isTerminated) {
            future.failed("ExecutorService is cancelled")
            return future
        }

        persistenceExecutors.execute {

            mItemDao.deleteAllItems()

            future.success(true)
        }

        return future
    }

    fun deleteDownloadableItems(downloadableItems: List<DownloadableItem>) : ListenableFuture<List<DownloadableItem>> {
        val future = ListenableFuture<List<DownloadableItem>>()

        if (persistenceExecutors.isShutdown || persistenceExecutors.isTerminated) {
            future.failed("ExecutorService is cancelled")
            return future
        }

        persistenceExecutors.execute {

            val deletedItems = ArrayList<DownloadableItem>()

            for (downloadableItem in downloadableItems) {
                val rowsAffected = mItemDao.deleteItem(downloadableItem)

                if (rowsAffected != 0) {
                    deletedItems.add(downloadableItem)
                }
            }

            future.success(deletedItems)
        }

        return future
    }

    fun deleteDownloadableItem(downloadableItem: DownloadableItem) : ListenableFuture<DownloadableItem> {
        val future = ListenableFuture<DownloadableItem>()

        if (persistenceExecutors.isShutdown || persistenceExecutors.isTerminated) {
            future.failed("ExecutorService is cancelled");
            return future
        }

        persistenceExecutors.execute {

            val rowsAffected = mItemDao.deleteItem(downloadableItem)

            if (rowsAffected != 0) {
                future.success(downloadableItem)
            }
            else {
                future.failed("Failed to delete item")
            }
        }

        return future
    }
}