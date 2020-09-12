package org.hugoandrade.rtpplaydownloader.network.download

import java.net.URL
import java.util.*
import kotlin.collections.ArrayList

class TSUtils
/**
 * Ensure this class is only used as a utility.
 */
private constructor() {

    init {
        throw AssertionError()
    }


    companion object {

        fun getM3U8Playlist(m3u8: String): String? {
            val validator = object : Validator<String> {
                override fun isValid(o: String): Boolean {
                    return o.startsWith("chunklist")
                }
            }
            return getM3U8Playlist(m3u8, validator)
        }

        fun getM3U8Playlist(m3u8: String, validator: Validator<String>): String? {
            try {
                val chunkListUrl = URL(m3u8)
                val s = Scanner(chunkListUrl.openStream())
                while (s.hasNext()) {
                    val line: String = s.next()
                    System.err.println("l - " + line)
                    if (validator.isValid(line)) return line
                }
            } catch (ignored: java.lang.Exception) {
                ignored.printStackTrace()
            }
            return null
        }

        fun getTSUrls(playlistUrl: String): List<String> {
            val validator = object : Validator<String> {
                override fun isValid(o: String): Boolean {
                    return o.endsWith(".ts")
                }
            }
            return getTSUrls(playlistUrl, validator)
        }

        fun getTSUrls(playlistUrl: String, validator: Validator<String>): List<String> {
            try {
                val tsUrls: MutableList<String> = ArrayList()
                val url = URL(playlistUrl)
                val s = Scanner(url.openStream())
                while (s.hasNext()) {
                    val line: String = s.next()
                    if (!validator.isValid(line)) continue
                    tsUrls.add(line)
                }
                return tsUrls
            } catch (ignored: java.lang.Exception) { }

            return ArrayList()
        }
    }

    interface Validator<T> {
        fun isValid(o : T) : Boolean
    }
}