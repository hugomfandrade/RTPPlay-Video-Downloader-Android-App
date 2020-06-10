package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTask

abstract class ParsingTask {

    val TAG : String = javaClass.simpleName

    var url: String? = null
    var mediaUrl: String? = null
    var thumbnailUrl: String? = null
    var filename: String? = null
    var isDownloading : Boolean = false

    lateinit var mDownloaderTaskListener: DownloaderTask

    var doCanceling: Boolean = false

    abstract fun isValid(url: String) : Boolean

    abstract fun parseMediaFile(url: String): Boolean

    abstract fun getMediaFileName(url: String, videoFile: String?): String
}