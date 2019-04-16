package org.hugoandrade.rtpplaydownloader.network

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

        fun isValidURL(urlText: String): Boolean {
            return try {
                val url = URL(urlText)
                "http" == url.protocol || "https" == url.protocol
            } catch (e: Exception) {
                false
            }
        }
    }
}