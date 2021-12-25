package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingData

abstract class ParsingMultiPartTask : ParsingTask {

    val tasks : ArrayList<ParsingTask> = ArrayList()
    val datas : ArrayList<ParsingData> = ArrayList()
}