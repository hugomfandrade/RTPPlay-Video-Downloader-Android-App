package org.hugoandrade.rtpplaydownloader.network.parsing.pagination

import org.hugoandrade.rtpplaydownloader.network.utils.NetworkUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.DataNode
import org.jsoup.nodes.Document
import java.io.IOException
import java.net.SocketTimeoutException

class RTPPlayPaginationParserTask : PaginationParserTask() {

    private var cacheDoc: Document? = null
    private var cachePage: Int = 1

    override fun isValid(urlString: String) : Boolean {

        if (!NetworkUtils.isValidURL(urlString)) {
            return false
        }

        val isFileType: Boolean = urlString.contains("www.rtp.pt/play")

        if (isFileType) {

            try {
                val doc: Document?

                try {
                    doc = Jsoup.connect(urlString).timeout(10000).get()
                } catch (ignored: IOException) {
                    return false
                }

                val episodeItems = doc?.getElementsByClass("episode-item")

                if (episodeItems != null && !episodeItems.isEmpty()) {

                    for (episodeItem in episodeItems.iterator()) {

                        episodeItem.attr("href") ?: continue

                        return true
                    }
                }
            }
            catch (e : java.lang.Exception) {
                e.printStackTrace()
            }
        }

        return false
    }

    override fun parsePagination(urlString: String): ArrayList<String> {

        val paginationUrl : ArrayList<String> = ArrayList()

        if (!NetworkUtils.isValidURL(urlString)) {
            return paginationUrl
        }

        val isFileType: Boolean = urlString.contains("www.rtp.pt/play")

        if (isFileType) {

            try {
                val doc: Document?

                try {
                    doc = Jsoup.connect(urlString).timeout(10000).get()
                } catch (ignored: IOException) {
                    return paginationUrl
                }

                cacheDoc = doc

                val episodeContainer = doc?.getElementsByAttributeValue("id", "listProgramsContent")

                if (episodeContainer == null || episodeContainer.isEmpty()) {
                    return paginationUrl
                }

                val episodeItems = episodeContainer.first().getElementsByClass("episode-item")

                if (episodeItems != null && !episodeItems.isEmpty()) {

                    for (episodeItem in episodeItems.iterator()) {

                        val href = episodeItem.attr("href") ?: continue

                        val episodeUrlString = "https://www.rtp.pt$href"

                        paginationUrl.add(episodeUrlString)
                    }
                }
            }
            catch (e : java.lang.Exception) {
                e.printStackTrace()
            }
        }

        setPaginationComplete(paginationUrl.size == 0)

        return paginationUrl
    }

    override fun parseMore(): ArrayList<String> {

        return cacheDoc?.let { parseMore(it) } ?: super.parseMore()
    }

    private fun parseMore(doc: Document): ArrayList<String> {

        val paginationUrl : ArrayList<String> = ArrayList()

        cachePage += 1

        val stamp = findHtmlOfLastClass(doc, "last_id")//: 234238
        val listDate = findInScript(doc, "RTPPLAY.currentHPListDate").replace("\"", "")//:
        val listQuery = findInScript(doc, "RTPPLAY.currentHPListQuery").replace("\"", "")//:
        val listProgram = findInScript(doc, "RTPPLAY.currentHPListProgram").replace("\"", "")//: 2383
        val listcategory = findInScript(doc, "RTPPLAY.currentHPListCategory").replace("\"", "")//:
        val listchannel = findInScript(doc, "RTPPLAY.currentHPListChannel").replace("\"", "")//:
        val listtype = findInScript(doc, "RTPPLAY.currentHPList").replace("\"", "")//: recent
        val page = findInScript(doc, "RTPPLAY.currentHPPageList").replace("\"", "")//: 2
        val type = findInScript(doc, "RTPPLAY.currentHPListType").replace("\"", "")//: tv
        val currentItemSelected = findInScript(doc, "RTPPLAY.currentItemSelected").replace("\"", "")//: 236098

        val req = "https://www.rtp.pt/play/bg_l_ep/?" +
                "stamp=" + stamp + "&" +
                "listDate=" + listDate + "&" +
                "listQuery=" + listQuery + "&" +
                "listProgram=" + listProgram + "&" +
                "listcategory=" + listcategory + "&" +
                "listchannel=" + listchannel + "&" +
                "listtype=" + "recent" + "&" +
                "page=" + cachePage + "&" +
                "type=" + type + "&" +
                "currentItemSelected=" + currentItemSelected

        try {

            val d: Document?

            try {
                d = Jsoup.connect(req).timeout(10000).get()
            } catch (ignored: IOException) {
                setPaginationComplete(paginationUrl.size == 0)
                return paginationUrl
            }

            // end temp

            val episodeItems = d.getElementsByClass("episode-item")

            if (episodeItems != null && !episodeItems.isEmpty()) {

                for (episodeItem in episodeItems.iterator()) {

                    val href = episodeItem.attr("href") ?: continue

                    val episodeUrlString = "https://www.rtp.pt$href"

                    paginationUrl.add(episodeUrlString)
                }
            }

        } catch (e: SocketTimeoutException) {
            e.printStackTrace()
        }

        setPaginationComplete(paginationUrl.size == 0)

        return paginationUrl
    }

    private fun findInScript(doc: Document, id: String): String {

        val scriptElements = doc.getElementsByTag("script")

        if (scriptElements != null) {

            for (scriptElement in scriptElements.iterator()) {

                for (dataNode: DataNode in scriptElement.dataNodes()) {

                    if (dataNode.wholeData.contains("$id = ")) {
                        return extractValue(dataNode, "$id = ", ";")
                    }
                }
            }
        }
        return ""
    }

    private fun extractValue(dataNode: DataNode, start: String, end: String): String {
        val first = dataNode.wholeData.substring(dataNode.wholeData.indexOf(start) + start.length)
        val second = first.substring(0, first.indexOf(end))
        return second
    }

    private fun findHtmlOfLastClass(doc: Document, className: String): String {

        val classElements = doc.getElementsByClass(className)

        if (classElements != null) {

            val classElement = classElements.last()

            if (classElement != null) {
                return classElement.html()
            }
        }
        return ""
    }
}

