package org.hugoandrade.downloader.utils

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL

class NetworkUtils
/**
 * Ensure this class is only used as a utility.
 */
private constructor() {

    init {
        throw AssertionError()
    }

    companion object {

        fun getDoc(url: String): Document? {
            return getDoc(url, 10000);
        }

        fun getDoc(url: String, millis: Int): Document? {
            return try {
                Jsoup.connect(url).timeout(millis).get()
            } catch (e : java.lang.Exception) {
                e.printStackTrace()
                null
            }
        }

        fun isValidURL(urlText: String): Boolean {
            return try {
                val url = URL(urlText)
                "http" == url.protocol || "https" == url.protocol
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}