package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTaskListener

abstract class ParsingTaskBase {

    val TAG : String = javaClass.simpleName

    var url: String? = null
    var mediaUrl: String? = null
    var thumbnailUrl: String? = null
    var filename: String? = null
    var isDownloading : Boolean = false

    lateinit var mDownloaderTaskListener: DownloaderTaskListener

    var doCanceling: Boolean = false

    abstract fun isValid(urlString: String) : Boolean

    abstract fun parseMediaFile(url: String): Boolean

    abstract fun getMediaFileName(url: String, videoFile: String?): String
}