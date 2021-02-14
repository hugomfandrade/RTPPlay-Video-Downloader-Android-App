package org.hugoandrade.rtpplaydownloader.network.download

import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingUtils
import org.hugoandrade.rtpplaydownloader.network.parsing.TSPlaylist
import org.hugoandrade.rtpplaydownloader.network.parsing.TSUrl
import java.net.URI
import java.net.URISyntaxException
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
            if (!getUrlWithoutParameters(m3u8).endsWith(".m3u8")) return null

            try {
                val chunkListUrl = URL(m3u8)
                val s = Scanner(chunkListUrl.openStream())
                while (s.hasNext()) {
                    val line: String = s.next()
                    if (validator.isValid(line)) return line
                }
            } catch (ignored: java.lang.Exception) {
                ignored.printStackTrace()
            }
            return null
        }

        fun getCompleteM3U8Playlist(playlistUrl: String): TSPlaylist? {
            if (!getUrlWithoutParameters(playlistUrl).endsWith(".m3u8")) return null

            try {
                val tsPlaylist = TSPlaylist()

                val baseUrl: String = playlistUrl.substring(0, playlistUrl.lastIndexOf("/") + 1)
                val url = URL(playlistUrl)
                val s = Scanner(url.openStream())
                while (s.hasNext()) {
                    val line: String = s.next()

                    if (!line.contains("#EXT-X-STREAM-INF")) continue
                    if (!line.contains("BANDWIDTH=")) continue
                    if (!line.contains("RESOLUTION=")) continue

                    val bandwidthFrom = line.substring(ParsingUtils.indexOfEx(line, "BANDWIDTH="))
                    val bandwidth = bandwidthFrom.substring(0, bandwidthFrom.indexOf(",")).toInt()

                    val resolutionFrom = line.substring(ParsingUtils.indexOfEx(line, "RESOLUTION="))
                    val resolutionTo = resolutionFrom.indexOf(",")
                    val resolution = resolutionFrom.substring(0, if (resolutionTo == -1) resolutionFrom.length else resolutionTo)

                    val resolutionArray = IntArray(2)
                    resolutionArray[0] = resolution.split("x")[0].toInt()
                    resolutionArray[1] = resolution.split("x")[1].toInt()

                    val nextLine = s.next()
                    val url = baseUrl + nextLine

                    tsPlaylist.add(resolution, TSUrl(url, bandwidth, resolutionArray))
                }
                return tsPlaylist
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }

            return null
        }

        fun getCompleteM3U8PlaylistWithoutBaseUrl(playlistUrl: String): TSPlaylist? {
            if (!getUrlWithoutParameters(playlistUrl).endsWith(".m3u8")) return null

            try {
                val tsPlaylist = TSPlaylist()

                val url = URL(playlistUrl)
                val s = Scanner(url.openStream())
                while (s.hasNext()) {
                    val line: String = s.next()

                    if (!line.contains("#EXT-X-STREAM-INF")) continue
                    if (!line.contains("BANDWIDTH=")) continue
                    if (!line.contains("RESOLUTION=")) continue

                    val bandwidthFrom = line.substring(ParsingUtils.indexOfEx(line, "BANDWIDTH="))
                    val bandwidth = bandwidthFrom.substring(0, bandwidthFrom.indexOf(",")).toInt()

                    val resolutionFrom = line.substring(ParsingUtils.indexOfEx(line, "RESOLUTION="))
                    val resolution = resolutionFrom.substring(0, resolutionFrom.indexOf(","))

                    val resolutionArray = IntArray(2)
                    resolutionArray[0] = resolution.split("x")[0].toInt()
                    resolutionArray[1] = resolution.split("x")[1].toInt()

                    val nextLine = s.next()
                    val url = nextLine

                    tsPlaylist.add(resolution, TSUrl(url, bandwidth, resolutionArray))
                }
                return tsPlaylist
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }

            return null
        }

        fun getTSUrls(playlistUrl: String): List<String> {
            val validator = object : Validator<String> {
                override fun isValid(o: String): Boolean {
                    return o.contains(".ts")
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

        @Throws(URISyntaxException::class)
        fun getUrlWithoutParameters(url: String): String {
            val uri = URI(url)
            return URI(uri.scheme,
                    uri.authority,
                    uri.path,
                    null,  // Ignore the query part of the input url
                    uri.fragment).toString()
        }
    }

    interface Validator<T> {
        fun isValid(o: T) : Boolean
    }
}