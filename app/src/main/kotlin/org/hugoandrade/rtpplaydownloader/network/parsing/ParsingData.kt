package org.hugoandrade.rtpplaydownloader.network.parsing

import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.ParsingTask
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationParserTask
import kotlin.collections.ArrayList

data class ParsingData(val tasks : ArrayList<ParsingTask>,
                       val paginationTask : PaginationParserTask?) {

    constructor(task : ParsingTask, paginationTask : PaginationParserTask?) :
            this(arrayListOf(task), paginationTask)

    constructor(task : ParsingTask) :
            this(arrayListOf(task), null)

    constructor(paginationTask: PaginationParserTask?) :
            this(ArrayList(), paginationTask)
}