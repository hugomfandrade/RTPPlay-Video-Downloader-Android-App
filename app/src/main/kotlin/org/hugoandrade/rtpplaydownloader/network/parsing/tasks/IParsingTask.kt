package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingUtils
import org.hugoandrade.rtpplaydownloader.network.utils.NetworkUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL

interface IParsingTask {

    var url: String?
    var mediaUrl: String?
    var thumbnailUrl: String?
    var filename: String?

    // check if url is supported
    fun isUrlSupported(url: String) : Boolean

    // check if url is valid - try to get 'Document' and call isValid(doc) method
    fun isValid(url: String) : Boolean {

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

    fun isValid(doc: Document) : Boolean {

        return try {
            val mediaUrl: String? = parseMediaUrl(doc)

            mediaUrl != null
        }
        catch (e : java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    fun parseMediaFile(url: String): Boolean {

        return try {
            val doc = Jsoup.connect(url).timeout(10000).get()
            parseMediaFile(doc)
        }
        catch (e : java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    fun parseMediaFile(doc: Document): Boolean {

        this.url = doc.baseUri()
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

    fun parseMediaUrl(doc: Document): String?

    fun parseMediaFileName(doc: Document): String {
        return ParsingUtils.getMediaFileName(doc, url?: null.toString(), mediaUrl)
    }

    fun parseThumbnailPath(doc: Document): String? {
        return ParsingUtils.getThumbnailFromTwitterMetadata(doc)
    }

}