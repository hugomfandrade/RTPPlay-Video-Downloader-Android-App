package org.hugoandrade.rtpplaydownloader.network.download

import android.os.Environment
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.hugoandrade.rtpplaydownloader.utils.NetworkUtils
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.URL

class SAPODownloaderTask : DownloaderTaskBase() {

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
        videoFileName = MediaUtils.getUniqueFilename(getVideoFileName(urlString, videoFile))

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
            if (MediaUtils.doesMediaFileExist(f)) {
                mDownloaderTaskListener.downloadFailed("file with same name already exists")
                return
            }
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
            android.util.Log.d(TAG, mue.message)
            mue.printStackTrace()
            mDownloaderTaskListener.downloadFailed(null)
        } catch (ioe: IOException) {
            android.util.Log.d(TAG, ioe.message)
            ioe.printStackTrace()
            mDownloaderTaskListener.downloadFailed(null)
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

            mDownloaderTaskListener.downloadFailed(null)
            doCanceling = false
            return true
        }
        return false
    }

    override fun isValid(urlString: String) : Boolean {

        if (!NetworkUtils.isValidURL(urlString)) {
            return false
        }

        val isFileType: Boolean =
                urlString.contains("videos.sapo.pt")

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

            val playerVideoElements = doc?.getElementsByAttributeValue("id", "player-video")

            if (playerVideoElements != null) {

                for (playerVideoElement in playerVideoElements.iterator()) {

                    val dataVideoLink : String = playerVideoElement.attr("data-video-link")

                    if (dataVideoLink.isEmpty()) continue

                    val location : String = when {
                        urlString.contains("http://") -> "http:"
                        urlString.contains("https://") -> "https:"
                        else -> ""
                    }

                    try {
                        val res : Connection.Response  = Jsoup.connect(location + dataVideoLink)
                                .ignoreContentType(true)
                                .timeout(10000)
                                .execute()

                        val url : String = res.url().toString()

                        if (url.isNotEmpty()) {
                            return url
                        }
                    } catch (e: SocketTimeoutException) {
                        e.printStackTrace()
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


            val titleElements = doc?.getElementsByTag("title")

            if (videoFile != null && titleElements != null && titleElements.size > 0) {

                val title: String = MediaUtils.getTitleAsFilename(titleElements.elementAt(0).text())
                        .replace(".SAPO.Videos", "")

                return "$title.mp4"
            }
        }
        catch (e : java.lang.Exception) {
            e.printStackTrace()
        }

        return videoFile?:urlString
    }
}