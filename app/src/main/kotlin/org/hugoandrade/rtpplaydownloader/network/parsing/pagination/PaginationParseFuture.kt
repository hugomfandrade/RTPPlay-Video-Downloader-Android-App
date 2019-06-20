package org.hugoandrade.rtpplaydownloader.network.parsing.pagination

import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTaskBase
import org.hugoandrade.rtpplaydownloader.utils.ListenableFutureImpl

class PaginationParseFuture(val urlString : String, val paginationTask : PaginationParserTaskBase) :

        ListenableFutureImpl<ArrayList<DownloaderTaskBase>>() {

}