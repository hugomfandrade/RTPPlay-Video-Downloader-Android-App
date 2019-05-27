package org.hugoandrade.rtpplaydownloader.network.download

import android.util.Log
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.SocketTimeoutException

class RTPPlayDownloaderMultiPartTask : DownloaderMultiPartTaskBase() {

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

        val urls: ArrayList<String> = getUrls(urlString)

        urls.forEach(action = { url ->
            Log.e(TAG, "parseMediaFile::" + url)
            val task = RTPPlayDownloaderTask()

            if (task.isValid(url) && task.parseMediaFile(url)) {
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

    private fun getUrls(urlString: String): ArrayList<String> {
        val urls = ArrayList<String>()

        // is Multi Part
        try {
            val doc: Document?

            try {
                doc = Jsoup.connect(urlString).timeout(10000).get()
            } catch (ignored: SocketTimeoutException) {
                return urls
            }

            val sectionParts = doc?.getElementsByClass("section-parts") ?: return urls

            for (sectionPart: Element in sectionParts.iterator()) {

                for (parts: Element in sectionPart.getElementsByClass("parts")) {

                    for (li: Element in parts.getElementsByTag("li")) {

                        if (li.hasClass("active")) {
                            urls.add(urlString)
                            continue
                        }
                        for (a: Element in li.getElementsByTag("a")) {

                            val href: String = a.attr("href")

                            if (href.isEmpty()) continue
                            // urls.add("https://www.rtp.pt$href")
                            urls.add(doc.location() + href)
                        }
                    }
                }
            }

        }
        catch (e : java.lang.Exception) {
            e.printStackTrace()
        }

        return urls
    }

    private fun getVideoFileName(urlString: String, videoFile: String?): String {
        try {
            val doc: Document?

            try {
                doc = Jsoup.connect(urlString).timeout(10000).get()
            } catch (ignored: SocketTimeoutException) {
                return videoFile ?: urlString
            }

            val titleElements = doc?.getElementsByTag("title")

            if (videoFile != null && titleElements != null && titleElements.size > 0) {

                val title: String = MediaUtils.getTitleAsFilename(titleElements.elementAt(0).text())
                        .replace(".RTP.Play.RTP", "")

                if (videoFile.indexOf(".mp4") >= 0) {  // is video file

                    return "$title.mp4"

                } else if (videoFile.indexOf(".mp3") >= 0) { // is audio file
                    return "$title.mp3"
                }

                return title
            }
        }
        catch (e : java.lang.Exception) {
            e.printStackTrace()
        }

        return videoFile?:urlString
    }

    private fun indexOfEx(string : String, subString: String) : Int {
        if (string.contains(subString)) {
            return string.indexOf(subString) + subString.length
        }
        return 0
    }
}