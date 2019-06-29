package org.hugoandrade.rtpplaydownloader.network.persistance

import org.hugoandrade.rtpplaydownloader.common.ContextView

interface PersistencePresenterOps : ContextView {

    fun onGetAllDownloadableEntries(downloadableEntries: List<DownloadableEntry>) {
        //No-op
    }

    fun onInsertDownloadableEntry(downloadableEntry: DownloadableEntry) {
        //No-op
    }

    fun onDeleteAllDownloadableEntries(downloadableEntries: List<DownloadableEntry>) {
        //No-op
    }

    fun onDeleteDownloadableEntry(downloadableEntry: DownloadableEntry) {
        //No-op
    }

    fun onUpdateDownloadableEntry(downloadableEntry: DownloadableEntry) {
        //No-op
    }

    fun onResetDatabase(wasSuccessfullyDeleted: Boolean) {
        //No-op
    }
}