package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

abstract class ParsingMultiPartTask : ParsingTask() {

    var tasks : ArrayList<ParsingTask> = ArrayList()
}