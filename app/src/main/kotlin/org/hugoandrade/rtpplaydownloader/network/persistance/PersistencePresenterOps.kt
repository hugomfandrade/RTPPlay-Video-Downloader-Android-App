package org.hugoandrade.rtpplaydownloader.network.persistance

import org.hugoandrade.rtpplaydownloader.common.ContextView
import org.hugoandrade.rtpplaydownloader.network.DownloadableItem

interface PersistencePresenterOps : ContextView {

    fun onDownloadableItemsRetrieved(downloadableItems: List<DownloadableItem>) {
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