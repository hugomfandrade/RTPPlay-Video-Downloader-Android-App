package org.hugoandrade.rtpplaydownloader.network

import org.hugoandrade.rtpplaydownloader.common.ContextView

interface DownloadManagerViewOps : ContextView {
    fun populateDownloadableItemsRecyclerView(downloadableItems: List<DownloadableItemAction>)
}