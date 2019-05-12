package org.hugoandrade.rtpplaydownloader.network

import org.hugoandrade.rtpplaydownloader.common.ContextView

interface DownloadManagerViewOps : ContextView {
    fun onParsingError(url: String, message : String)
    fun onParsingSuccessful(item: DownloadableItem)
    fun populateDownloadableItemsRecyclerView(downloadableItems: List<DownloadableItem>)
}