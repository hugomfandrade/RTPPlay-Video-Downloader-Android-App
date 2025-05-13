package org.hugoandrade.rtpplaydownloader.network

import androidx.lifecycle.LiveData
import org.hugoandrade.downloader.parsing.*
import org.hugoandrade.downloader.parsing.pagination.*
import org.hugoandrade.rtpplaydownloader.utils.ListenableFuture

interface DownloadManagerAPI {

    fun getItems(): LiveData<ArrayList<DownloadableItemAction>>
    fun parseUrl(url: String): ListenableFuture<ParsingTaskResult>
    fun parsePagination(url: String, paginationTask: PaginationParserTask): ListenableFuture<ArrayList<ParsingData>>
    fun parseMore(url: String, paginationTask: PaginationParserTask): ListenableFuture<ArrayList<ParsingData>>
    fun download(parsingData: ParsingData): ListenableFuture<DownloadableItemAction>

    fun retrieveItemsFromDB()
    fun archive(downloadableItem: AndroidDownloadableItem)
    fun emptyDB()
}