package org.hugoandrade.rtpplaydownloader.network.parsing.pagination

import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.ParsingTaskBase
import org.hugoandrade.rtpplaydownloader.utils.ListenableFutureImpl

class PaginationParseFuture(val urlString : String, val paginationTask : PaginationParserTaskBase) :

        ListenableFutureImpl<ArrayList<ParsingTaskBase>>() {

}