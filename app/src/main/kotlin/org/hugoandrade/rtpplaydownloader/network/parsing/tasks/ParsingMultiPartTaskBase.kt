package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

abstract class ParsingMultiPartTaskBase : ParsingTaskBase() {

    var tasks : ArrayList<ParsingTaskBase> = ArrayList()
}