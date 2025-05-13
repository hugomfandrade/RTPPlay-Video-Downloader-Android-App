package org.hugoandrade.downloader.parsing.tasks

import org.hugoandrade.downloader.parsing.ParsingData

abstract class ParsingMultiPartTask : ParsingTask {

    val tasks : ArrayList<ParsingTask> = ArrayList()
    val datas : ArrayList<ParsingData> = ArrayList()
}