package org.hugoandrade.rtpplaydownloader.network.download

import android.os.Build
import android.os.Environment
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.hugoandrade.rtpplaydownloader.utils.NetworkUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL
import org.jsoup.nodes.DataNode
import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.SocketTimeoutException

class RTPPlayDownloaderTask : DownloaderTaskBase() {

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
            val size = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                huc.contentLengthLong
            } else {
                huc.contentLength.toLong()
            }

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
                var progress = len.toLong()
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
                    if (tryToCancelIfNeeded(fos, inputStream, f)) {
                        // do cancelling while paused
                        return
                    }
                    mDownloaderTaskListener.onProgress(progress.toFloat() / size.toFloat())
                    mDownloaderTaskListener.onProgress(progress, size)
                }
            }
            mDownloaderTaskListener.downloadFinished(f)

            fos.close()

        } catch (mue: MalformedURLException) {
            mue.printStackTrace()
            mDownloaderTaskListener.downloadFailed(null)
        } catch (ioe: IOException) {
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

        val isFileType: Boolean = urlString.contains("www.rtp.pt/play")

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

            val scriptElements = doc?.getElementsByTag("script")

            if (scriptElements != null) {

                for (scriptElement in scriptElements.iterator()) {

                    for (dataNode: DataNode in scriptElement.dataNodes()) {
                        if (dataNode.wholeData.contains("RTPPlayer")) {

                            val scriptText: String = dataNode.wholeData

                            try {

                                val rtpPlayerSubString: String = scriptText.substring(indexOfEx(scriptText, "RTPPlayer({"), scriptText.lastIndexOf("})"))

                                if (rtpPlayerSubString.indexOf(".mp4") >= 0) {  // is video file

                                    if (rtpPlayerSubString.indexOf("fileKey: \"") >= 0) {

                                        val link: String = rtpPlayerSubString.substring(
                                                indexOfEx(rtpPlayerSubString, "fileKey: \""),
                                                indexOfEx(rtpPlayerSubString, "fileKey: \"") + rtpPlayerSubString.substring(indexOfEx(rtpPlayerSubString, "fileKey: \"")).indexOf("\","))


                                        return "http://cdn-ondemand.rtp.pt$link"
                                    }

                                } else if (rtpPlayerSubString.indexOf(".mp3") >= 0) { // is audio file

                                    if (rtpPlayerSubString.indexOf("file: \"") >= 0) {

                                        return rtpPlayerSubString.substring(
                                                indexOfEx(rtpPlayerSubString, "file: \""),
                                                indexOfEx(rtpPlayerSubString, "file: \"") + rtpPlayerSubString.substring(indexOfEx(rtpPlayerSubString, "file: \"")).indexOf("\","))

                                    }
                                }
                            } catch (parsingException: java.lang.Exception) {

                            }
                        }
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