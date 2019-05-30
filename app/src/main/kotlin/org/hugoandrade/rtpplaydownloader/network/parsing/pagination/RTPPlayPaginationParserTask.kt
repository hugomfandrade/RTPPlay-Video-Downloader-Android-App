package org.hugoandrade.rtpplaydownloader.network.parsing.pagination

import org.hugoandrade.rtpplaydownloader.utils.NetworkUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.DataNode
import org.jsoup.nodes.Document
import java.net.SocketTimeoutException

class RTPPlayPaginationParserTask : PaginationParserTaskBase() {

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
                } catch (ignored: SocketTimeoutException) {
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
                } catch (ignored: SocketTimeoutException) {
                    return paginationUrl
                }

                // temp

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
                        "page=" + page + "&" +
                        "type=" + type + "&" +
                        "currentItemSelected=" + currentItemSelected

                android.util.Log.e(TAG, "req = " + req)

                // end temp

                val episodeItems = doc?.getElementsByClass("episode-item")

                if (episodeItems != null && !episodeItems.isEmpty()) {

                    for (episodeItem in episodeItems.iterator()) {

                        val href = episodeItem.attr("href") ?: continue

                        val episodeUrlString = "https://www.rtp.pt$href"

                        paginationUrl.add(episodeUrlString)

                        /*
                        getDocumentInUrl(window.location.origin + , function(doc) {

                            if (isValid(doc) === true) {
                                sendIsAllValidMessage(true);
                            }
                            else {
                                var nextIt = it + 1;

                                if (nextIt === episodeItems.length) {
                                    sendIsAllValidMessage(false);
                                }
                                else {
                                    isEpisodeItemValid(episodeItems, nextIt);
                                }
                            }
                        });*/

                    }
                }
            }
            catch (e : java.lang.Exception) {
                e.printStackTrace()
            }
        }

        return paginationUrl
    }

    private fun findInScript(doc: Document, id: String): String {

        val scriptElements = doc.getElementsByTag("script")

        if (scriptElements != null) {

            for (scriptElement in scriptElements.iterator()) {

                for (dataNode: DataNode in scriptElement.dataNodes()) {

                    if (dataNode.wholeData.contains(id + " = ")) {
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

