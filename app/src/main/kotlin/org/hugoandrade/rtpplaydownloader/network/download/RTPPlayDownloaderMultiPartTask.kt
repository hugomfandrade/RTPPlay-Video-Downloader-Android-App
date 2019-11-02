package org.hugoandrade.rtpplaydownloader.network.download

import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.SocketTimeoutException

class RTPPlayDownloaderMultiPartTask : DownloaderMultiPartTaskBase() {

    override fun getMediaFileName(urlString: String, videoFile: String?): String {
        // do nothing
        return null.toString()
    }

    override fun downloadMediaFile(listener: DownloaderTaskListener) {
        // do nothing
    }

    override fun cancel() {
        // do nothing
    }

    override fun resume() {
        // do nothing
    }

    override fun pause() {
        // do nothing
    }

    override fun parseMediaFile(urlString: String): Boolean {

        tasks.clear()

        val urls: ArrayList<RTPPlayMultiPartMetadata> = getUrls(urlString)

        urls.forEach(action = { url ->
            val task = RTPPlayDownloaderTask()

            if (task.isValid(url.urlString) && task.parseMediaFile(url.urlString)) {
                val part = url.suffix
                val originalFilename = task.getMediaFileName(url.urlString, task.mediaUrl)

                if (part != null) {
                    val lastDot = originalFilename.lastIndexOf(".")
                    val preFilename = originalFilename.substring(0, lastDot)
                    val extFilename = originalFilename.substring(lastDot, originalFilename.length)
                    task.mediaFileName = MediaUtils.getUniqueFilenameAndLock("$preFilename.$part$extFilename")
                }
                tasks.add(task)
            }
        })

        return tasks.size != 0
    }

    override fun isValid(urlString: String) : Boolean {
        if (!RTPPlayDownloaderTask().isValid(urlString)) {
            return false
        }

        // is Multi Part
        try {
            val doc: Document?

            try {
                doc = Jsoup.connect(urlString).timeout(10000).get()
            } catch (ignored: SocketTimeoutException) {
                return false
            }

            val sectionParts = doc?.getElementsByClass("section-parts") ?: return false

            for (sectionPart: Element in sectionParts.iterator()) {

                for (parts: Element in sectionPart.getElementsByClass("parts")) {

                    for (li: Element in parts.getElementsByTag("li")) {

                        /*if (li.hasClass("active")) {
                            return true
                        }*/

                        for (a: Element in li.getElementsByTag("a")) {

                            val href: String = a.attr("href")

                            if (href.isEmpty()) continue
                            return true
                        }
                    }
                }
            }

        }
        catch (e : java.lang.Exception) {
            e.printStackTrace()
        }

        return false
    }

    private fun getUrls(urlString: String): ArrayList<RTPPlayMultiPartMetadata> {
        val urls = ArrayList<String>()
        val urlsMetadata = ArrayList<RTPPlayMultiPartMetadata>()

        // is Multi Part
        try {
            val doc: Document?

            try {
                doc = Jsoup.connect(urlString).timeout(10000).get()
            } catch (ignored: SocketTimeoutException) {
                return urlsMetadata
            }

            val sectionParts = doc?.getElementsByClass("section-parts") ?: return urlsMetadata

            for (sectionPart: Element in sectionParts.iterator()) {

                for (parts: Element in sectionPart.getElementsByClass("parts")) {

                    for (li: Element in parts.getElementsByTag("li")) {

                        if (li.hasClass("active")) {

                            for (span: Element in li.getElementsByTag("span")) {

                                val part: String = span.html().capitalize()
                                        .replace("PARTE", "P")
                                        .replace("\\s+","")
                                        .replace(" ","")

                                urls.add(urlString)
                                urlsMetadata.add(RTPPlayMultiPartMetadata(urlString, part))
                            }
                            // urls.add(urlString)
                            continue
                        }
                        for (a: Element in li.getElementsByTag("a")) {

                            val href: String = a.attr("href")
                            val part: String = a.html().capitalize()
                                    .replace("PARTE", "P")
                                    .replace("\\s+","")
                                    .replace(" ","")

                            if (href.isEmpty()) continue
                            urls.add("https://www.rtp.pt$href")
                            urlsMetadata.add(RTPPlayMultiPartMetadata("https://www.rtp.pt$href", part))
                        }
                    }
                }
            }

        }
        catch (e : java.lang.Exception) {
            e.printStackTrace()
        }

        return urlsMetadata
    }
}