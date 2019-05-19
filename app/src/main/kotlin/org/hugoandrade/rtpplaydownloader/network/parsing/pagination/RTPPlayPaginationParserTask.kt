package org.hugoandrade.rtpplaydownloader.network.parsing.pagination

import org.hugoandrade.rtpplaydownloader.utils.NetworkUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.SocketTimeoutException

class RTPPlayPaginationParserTask : PaginationParserTaskBase() {

    override fun isValid(urlString: String) : Boolean {
        android.util.Log.e(TAG, "isValid")

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

                android.util.Log.e(TAG, "try get episode-items")

                val episodeItems = doc?.getElementsByClass("episode-item")

                if (episodeItems != null && !episodeItems.isEmpty()) {

                    for (episodeItem in episodeItems.iterator()) {

                        val href = episodeItem.attr("href") ?: continue

                        val episodeUrlString = doc.location() + href

                        android.util.Log.e(TAG, href)
                        android.util.Log.e(TAG, episodeUrlString)

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

        return false
    }
}