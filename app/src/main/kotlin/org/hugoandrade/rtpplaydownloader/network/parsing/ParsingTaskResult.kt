package org.hugoandrade.rtpplaydownloader.network.parsing

import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationParserTask

data class ParsingTaskResult(val parsingDatas : ArrayList<ParsingData>,
                             val paginationTask : PaginationParserTask?) {

    constructor(parsingData: ParsingData, paginationTask : PaginationParserTask?) :
            this(arrayListOf(parsingData), paginationTask)
}