package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingData
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingUtils
import org.hugoandrade.rtpplaydownloader.network.utils.NetworkUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL

interface ParsingTask {

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

    fun parseMediaFile(url: String): ParsingData? {

        return try {
            val doc = Jsoup.connect(url).timeout(10000).get()
            parseMediaFile(doc)
        }
        catch (e : java.lang.Exception) {
            e.printStackTrace()
            null
        }
    }

    fun parseMediaFile(doc: Document): ParsingData? {

        val url = doc.baseUri()
        val mediaUrl = parseMediaUrl(doc) ?: return null
        val filename = parseMediaFileName(doc, mediaUrl)
        val thumbnailUrl = parseThumbnailPath(doc)

        try {
            URL(mediaUrl)
        }
        catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        return ParsingData(url, mediaUrl, filename, thumbnailUrl)
    }

    fun parseMediaUrl(doc: Document): String?

    fun parseMediaFileName(doc: Document, mediaUrl: String): String {
        return ParsingUtils.getMediaFileName(doc, doc.baseUri()?: null.toString(), mediaUrl)
    }

    fun parseThumbnailPath(doc: Document): String? {
        return ParsingUtils.getThumbnailFromTwitterMetadata(doc)
    }

}