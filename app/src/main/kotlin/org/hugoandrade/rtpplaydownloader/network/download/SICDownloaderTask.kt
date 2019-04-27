package org.hugoandrade.rtpplaydownloader.network.download

import android.os.Environment
import org.hugoandrade.rtpplaydownloader.utils.NetworkUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL
import org.jsoup.nodes.Element
import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.text.Normalizer

class SICDownloaderTask : DownloaderTaskBase() {

    private lateinit var mDownloaderTaskListener: DownloaderTaskListener

    private var doCanceling: Boolean = false

    override fun cancel() {
        doCanceling = true
    }

    override fun resume() {
        isDownloading = true
    }

    override fun pause() {
        isDownloading = false
    }

    override fun parseMediaFile(urlString: String): Boolean {

        videoFile = getVideoFile(urlString) ?: return false
        videoFileName = getVideoFileName(urlString, videoFile)

        try {
            URL(videoFile)
        }
        catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        return true
    }

    override fun downloadMediaFile(listener: DownloaderTaskListener) {

        mDownloaderTaskListener = listener

        val u: URL?
        var inputStream: InputStream? = null

        try {
            u = URL(videoFile)
            inputStream = u.openStream()
            val huc = u.openConnection() as HttpURLConnection //to know the size of video
            val size = huc.contentLength

            val storagePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString()
            val f = File(storagePath, videoFileName)
            mDownloaderTaskListener.downloadStarted(f)

            val fos = FileOutputStream(f)
            val buffer = ByteArray(1024)
            if (inputStream != null) {
                var len = inputStream.read(buffer)
                var progress = len
                while (len > 0) {

                    if (tryToCancelIfNeeded(fos, inputStream, f)) {
                        // do cancelling
                        return
                    }

                    while (!isDownloading){
                        // pause

                        if (tryToCancelIfNeeded(fos, inputStream, f)) {
                            // do cancelling while paused
                            return
                        }
                    }

                    fos.write(buffer, 0, len)
                    len = inputStream.read(buffer)
                    progress += len
                    mDownloaderTaskListener.onProgress(progress.toFloat() / size.toFloat())
                }
            }
            mDownloaderTaskListener.downloadFinished(f)

            fos.close()

        } catch (mue: MalformedURLException) {
            mue.printStackTrace()
            mDownloaderTaskListener.downloadFailed()
        } catch (ioe: IOException) {
            ioe.printStackTrace()
            mDownloaderTaskListener.downloadFailed()
        } finally {
            try {
                inputStream?.close()
            } catch (ioe: IOException) {
                // just going to ignore this one
            }
        }
    }

    private fun tryToCancelIfNeeded(fos: FileOutputStream, inputStream: InputStream, f: File): Boolean {

        if (doCanceling) {
            fos.close()
            try {
                inputStream.close()
            } catch (ioe: IOException) {
                // just going to ignore this one
            }
            f.delete()

            mDownloaderTaskListener.downloadFailed()
            return true
        }
        return false
    }

    override fun isValid(urlString: String) : Boolean {

        if (!NetworkUtils.isValidURL(urlString)) {
            return false
        }

        val isFileType: Boolean =
                urlString.contains("sicradical.sapo.pt") ||
                urlString.contains("sicradical.pt") ||
                urlString.contains("sicnoticias.sapo.pt") ||
                urlString.contains("sicnoticias.pt") ||
                urlString.contains("sic.sapo.pt") ||
                urlString.contains("sic.pt")

        if (isFileType) {

            val videoFile: String? = getVideoFile(urlString)

            return videoFile != null
        }

        return false
    }

    private fun getVideoFile(urlString: String): String? {
        try {
            val doc: Document?

            try {
                doc = Jsoup.connect(urlString).timeout(10000).get()
            } catch (ignored: SocketTimeoutException) {
                return null
            }

            val videoElements = doc?.getElementsByTag("video")

            if (videoElements != null) {
                for (videoElement in videoElements.iterator()) {

                    for (sourceElement: Element in videoElement.getElementsByTag("source")) {

                        val src : String = sourceElement.attr("src")
                        val type: String = sourceElement.attr("type")

                        if (src.isEmpty() || type.isEmpty()) continue

                        val location : String = when {
                            urlString.contains("http://") -> "http:"
                            urlString.contains("https://") -> "https:"
                            else -> ""
                        }

                        return location + src
                    }
                }
            }
        }
        catch (e : java.lang.Exception) {
            e.printStackTrace()
        }

        return null
    }

    private fun getVideoFileName(urlString: String, videoFile: String?): String {

        try {
            val doc: Document?

            try {
                doc = Jsoup.connect(urlString).timeout(10000).get()
            } catch (ignored: SocketTimeoutException) {
                return videoFile ?: urlString
            }

            val videoElements = doc?.getElementsByTag("video")
            var type: String? = null

            if (videoElements != null) {
                for (videoElement in videoElements.iterator()) {

                    for (sourceElement: Element in videoElement.getElementsByTag("source")) {

                        val src : String = sourceElement.attr("src")
                        type = sourceElement.attr("type")

                        if (src.isEmpty() || type.isEmpty()) continue
                        break
                    }
                }
            }

            val titleElements = doc?.getElementsByTag("title")

            if (videoFile != null && titleElements != null && titleElements.size > 0) {

                var title = titleElements.elementAt(0).text()

                title = title.replace('-', ' ')
                        .replace(':', ' ')
                        .replace("\\s{2,}".toRegex(), " ")
                        .trim()
                        .replace(' ', '.')
                        .replace(' ', '.')
                title = Normalizer.normalize(title, Normalizer.Form.NFKD)
                title = title
                        .replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")
                        .replace("SIC.Noticias.|.", "")
                        .replace("SIC.Radical.|.", "")
                        .replace("SIC.|.", "")

                if (checkNotNull(type?.contains("video/mp4"))) {  // is video file
                    return "$title.mp4"
                }

                return title
            }
        }
        catch (e : java.lang.Exception) {
            e.printStackTrace()
        }

        return videoFile?:urlString
    }
}