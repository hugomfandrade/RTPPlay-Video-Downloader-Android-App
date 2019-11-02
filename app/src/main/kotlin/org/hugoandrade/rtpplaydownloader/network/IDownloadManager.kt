package org.hugoandrade.rtpplaydownloader.network

import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTaskBase
import org.hugoandrade.rtpplaydownloader.network.parsing.ParseFuture
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationParseFuture
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationParserTaskBase

interface IDownloadManager {

    fun onCreate(viewOps: DownloadManagerViewOps)
    fun onConfigurationChanged(viewOps: DownloadManagerViewOps)
    fun onDestroy()

    fun parseUrl(url: String): ParseFuture
    fun parsePagination(url: String, paginationTask: PaginationParserTaskBase): PaginationParseFuture
    fun parseMore(url: String, paginationTask: PaginationParserTaskBase): PaginationParseFuture
    fun download(task: DownloaderTaskBase)

    fun retrieveItemsFromDB()
    fun archive(downloadableItem: DownloadableItem)
    fun emptyDB()
}