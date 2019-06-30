package org.hugoandrade.rtpplaydownloader.network.parsing

import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTaskBase
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationParserTaskBase
import kotlin.collections.ArrayList

data class ParsingData(val tasks : ArrayList<DownloaderTaskBase>,
                       val paginationTask : PaginationParserTaskBase?) {

    constructor(task : DownloaderTaskBase, paginationTask : PaginationParserTaskBase?) :
            this(arrayListOf(task), paginationTask)

    constructor(task : DownloaderTaskBase) :
            this(arrayListOf(task), null)

    constructor(paginationTask: PaginationParserTaskBase?) :
            this(ArrayList(), paginationTask)
}