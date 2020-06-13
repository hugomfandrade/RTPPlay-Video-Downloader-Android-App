package org.hugoandrade.rtpplaydownloader.network.parsing.pagination

import org.hugoandrade.rtpplaydownloader.network.utils.NetworkUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URL

class SICPaginationParserTask : PaginationParserTask() {

    private var cacheDoc: Document? = null

    override fun isValid(urlString: String) : Boolean {

        if (!NetworkUtils.isValidURL(urlString)) {
            return false
        }

        val isFileType: Boolean = urlString.contains("sicradical.sapo.pt") ||
                        urlString.contains("sicradical.pt") ||
                        urlString.contains("sicnoticias.sapo.pt") ||
                        urlString.contains("sicnoticias.pt") ||
                        urlString.contains("sic.sapo.pt") ||
                        urlString.contains("sic.pt")
        try {
            URL(urlString)
        }
        catch (e : Exception) {
            return false
        }

        if (isFileType) {

            try {
                val doc: Document?

                try {
                    doc = Jsoup.connect(urlString).timeout(10000).get()
                } catch (ignored: IOException) {
                    return false
                }

                val containers = doc?.getElementsByClass("categoryList")

                if (containers == null || containers.isEmpty()) return false

                for (container in containers.iterator()) {

                    val lis = container.getElementsByTag("li")

                    if (lis == null || lis.isEmpty()) return false

                    for (li in lis.iterator()) {

                        val articles = li.getElementsByTag("article")

                        if (articles == null || articles.isEmpty()) return false

                        for (article in articles.iterator()) {

                            val figures = article.getElementsByTag("figure")

                            if (figures == null || figures.isEmpty()) return false

                            for (figure in figures.iterator()) {

                                val episodeItems = figure.getElementsByTag("a")

                                if (episodeItems == null || episodeItems.isEmpty()) return false

                                for (episodeItem in episodeItems.iterator()) {

                                    episodeItem.attr("href") ?: continue

                                    return true
                                }
                            }
                        }
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

        val isFileType: Boolean = urlString.contains("sicradical.sapo.pt") ||
                urlString.contains("sicradical.pt") ||
                urlString.contains("sicnoticias.sapo.pt") ||
                urlString.contains("sicnoticias.pt") ||
                urlString.contains("sic.sapo.pt") ||
                urlString.contains("sic.pt")

        val url: URL
        try {
            url = URL(urlString)
        }
        catch (e : Exception) {
            return paginationUrl
        }

        if (isFileType) {

            try {
                val doc: Document?

                try {
                    doc = Jsoup.connect(urlString).timeout(10000).get()
                } catch (ignored: IOException) {
                    return paginationUrl
                }

                cacheDoc = doc

                val containers = doc?.getElementsByClass("categoryList")

                if (containers == null || containers.isEmpty()) return paginationUrl

                for (container in containers.iterator()) {

                    val lis = container.getElementsByTag("li")

                    if (lis == null || lis.isEmpty()) return paginationUrl

                    for (li in lis.iterator()) {

                        val articles = li.getElementsByTag("article")

                        if (articles == null || articles.isEmpty()) return paginationUrl

                        for (article in articles.iterator()) {

                            val figures = article.getElementsByTag("figure")

                            if (figures == null || figures.isEmpty()) return paginationUrl

                            for (figure in figures.iterator()) {

                                val episodeItems = figure.getElementsByTag("a")

                                if (episodeItems == null || episodeItems.isEmpty()) return paginationUrl

                                for (episodeItem in episodeItems.iterator()) {

                                    val href = episodeItem.attr("href") ?: continue

                                    val episodeUrlString = url.protocol + "://" + url.host + href

                                    paginationUrl.add(episodeUrlString)
                                }
                            }
                        }
                    }
                }
            }
            catch (e : java.lang.Exception) {
                e.printStackTrace()
            }
        }

        setPaginationComplete(true)

        return paginationUrl
    }
}

