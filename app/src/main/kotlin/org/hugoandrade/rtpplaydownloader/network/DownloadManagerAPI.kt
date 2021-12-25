package org.hugoandrade.rtpplaydownloader.network

import androidx.lifecycle.LiveData
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingData
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingTaskResult
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationParserTask
import org.hugoandrade.rtpplaydownloader.utils.ListenableFuture

interface DownloadManagerAPI {

    fun getItems(): LiveData<ArrayList<DownloadableItemAction>>
    fun parseUrl(url: String): ListenableFuture<ParsingTaskResult>
    fun parsePagination(url: String, paginationTask: PaginationParserTask): ListenableFuture<ArrayList<ParsingData>>
    fun parseMore(url: String, paginationTask: PaginationParserTask): ListenableFuture<ArrayList<ParsingData>>
    fun download(parsingData: ParsingData): ListenableFuture<DownloadableItemAction>

    fun retrieveItemsFromDB()
    fun archive(downloadableItem: DownloadableItem)
    fun emptyDB()
}