package org.hugoandrade.rtpplaydownloader.network.parsing

import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.ParsingTaskBase
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationParserTaskBase
import kotlin.collections.ArrayList

data class ParsingData(val tasks : ArrayList<ParsingTaskBase>,
                       val paginationTask : PaginationParserTaskBase?) {

    constructor(task : ParsingTaskBase, paginationTask : PaginationParserTaskBase?) :
            this(arrayListOf(task), paginationTask)

    constructor(task : ParsingTaskBase) :
            this(arrayListOf(task), null)

    constructor(paginationTask: PaginationParserTaskBase?) :
            this(ArrayList(), paginationTask)
}