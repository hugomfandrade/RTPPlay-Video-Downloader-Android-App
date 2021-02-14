package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTask
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingUtils
import org.hugoandrade.rtpplaydownloader.network.utils.NetworkUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL

abstract class ParsingTask {

    val TAG : String = javaClass.simpleName

    var url: String? = null
    var mediaUrl: String? = null
    var thumbnailUrl: String? = null
    var filename: String? = null
    var isDownloading : Boolean = false

    lateinit var mDownloaderTaskListener: DownloaderTask

    var doCanceling: Boolean = false

    open fun isUrlSupported(url: String) : Boolean {
        return true
    }

    open fun isValid(url: String) : Boolean {

        if (!NetworkUtils.isValidURL(url)) return false

        if (!isUrlSupported(url)) return false

        try {
            val doc = NetworkUtils.getDoc(url) ?: return false
            return isValid(doc)
        }
        catch (e : java.lang.Exception) {
            e.printStackTrace()
            return false
        }
    }

    open fun isValid(doc: Document) : Boolean {

        return try {
            val mediaUrl: String? = parseMediaUrl(doc)

            mediaUrl != null
        }
        catch (e : java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    open fun parseMediaFile(url: String): Boolean {

        this.url = url

        try {
            val doc = Jsoup.connect(url).timeout(10000).get()
            return parseMediaFile(doc)
        }
        catch (e : java.lang.Exception) {
            e.printStackTrace()
            return false
        }
    }

    open fun parseMediaFile(doc: Document): Boolean {

        this.mediaUrl = parseMediaUrl(doc) ?: return false
        this.filename = parseMediaFileName(doc)
        this.thumbnailUrl = parseThumbnailPath(doc)

        try {
            URL(mediaUrl)
        }
        catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        return true
    }

    abstract fun parseMediaUrl(doc: Document): String?

    protected open fun parseMediaFileName(doc: Document): String {
        return ParsingUtils.getMediaFileName(doc, url?: null.toString(), mediaUrl)
    }

    protected open fun parseThumbnailPath(doc: Document): String? {
        return ParsingUtils.getThumbnailFromTwitterMetadata(doc)
    }
}