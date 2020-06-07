package org.hugoandrade.rtpplaydownloader.network.persistence

import org.hugoandrade.rtpplaydownloader.network.DownloadableItem

@Deprecated(message = "user room instead")
interface PersistencePresenterOps {

    fun onDownloadableItemsRetrieved(downloadableItems: List<DownloadableItem>) {
        //No-op
    }

    fun onArchivedDownloadableItemsRetrieved(downloadableItems: List<DownloadableItem>) {
        //No-op
    }

    fun onDownloadableItemInserted(downloadableItem: DownloadableItem?) {
        //No-op
    }

    fun onDownloadableItemsDeleted(downloadableItems: List<DownloadableItem>) {
        //No-op
    }

    fun onDownloadableItemDeleted(downloadableItem: DownloadableItem?) {
        //No-op
    }

    fun onDownloadableItemUpdated(downloadableItem: DownloadableItem?) {
        //No-op
    }

    fun onDatabaseReset(wasSuccessfullyDeleted: Boolean) {
        //No-op
    }
}