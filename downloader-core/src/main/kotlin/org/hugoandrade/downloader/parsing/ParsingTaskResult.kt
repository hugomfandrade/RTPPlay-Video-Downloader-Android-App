package org.hugoandrade.downloader.parsing

import org.hugoandrade.downloader.parsing.pagination.PaginationParserTask

data class ParsingTaskResult(val parsingDatas : ArrayList<ParsingData>,
                             val paginationTask : PaginationParserTask?) {

    constructor(parsingData: ParsingData, paginationTask : PaginationParserTask?) :
            this(arrayListOf(parsingData), paginationTask)
}