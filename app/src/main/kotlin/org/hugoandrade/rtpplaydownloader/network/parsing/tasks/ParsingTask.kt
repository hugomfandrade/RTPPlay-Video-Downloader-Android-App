package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTask
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingUtils
import org.hugoandrade.rtpplaydownloader.network.utils.NetworkUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
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

    open fun isValid(url: String) : Boolean {

        if (!NetworkUtils.isValidURL(url)) {
            return false
        }

        try {
            val doc = Jsoup.connect(url).timeout(10000).get()
            val mediaUrl: String? = parseMediaUrl(doc)

            return mediaUrl != null
        }
        catch (e : java.lang.Exception) {
            e.printStackTrace()
            return false
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

    open fun parseMediaFile(file: File): Boolean {

        try {
            val doc = Jsoup.parse(file, null)
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