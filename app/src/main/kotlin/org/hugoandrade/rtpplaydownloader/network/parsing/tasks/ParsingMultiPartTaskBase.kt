package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

abstract class ParsingMultiPartTaskBase : ParsingTask() {

    var tasks : ArrayList<ParsingTask> = ArrayList()
}