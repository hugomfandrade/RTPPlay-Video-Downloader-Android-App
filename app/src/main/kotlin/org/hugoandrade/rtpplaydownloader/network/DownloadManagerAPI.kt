package org.hugoandrade.rtpplaydownloader.network

import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.ParsingTaskBase
import org.hugoandrade.rtpplaydownloader.network.parsing.ParseFuture
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationParseFuture
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationParserTaskBase

interface DownloadManagerAPI {

    fun attachCallback(viewOps: DownloadManagerViewOps)

    fun parseUrl(url: String): ParseFuture
    fun parsePagination(url: String, paginationTask: PaginationParserTaskBase): PaginationParseFuture
    fun parseMore(url: String, paginationTask: PaginationParserTaskBase): PaginationParseFuture
    fun download(task: ParsingTaskBase)

    fun retrieveItemsFromDB()
    fun archive(downloadableItem: DownloadableItem)
    fun emptyDB()
}