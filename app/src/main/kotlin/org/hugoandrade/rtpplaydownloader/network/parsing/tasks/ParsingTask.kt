package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

abstract class ParsingTask : IParsingTask {

    val TAG : String = javaClass.simpleName

    override var url: String? = null
    override var mediaUrl: String? = null
    override var thumbnailUrl: String? = null
    override var filename: String? = null
}