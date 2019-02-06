package org.hugoandrade.rtpplaydownloader.network

import android.os.Environment
import android.util.Log
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL
import org.jsoup.nodes.DataNode
import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.text.Normalizer


class RTPPlayDownloaderTask : DownloaderTaskBase() {

    override fun download(listener: DownloaderTaskListener, urlString: String) {

        val videoFile: String = getVideoFile(urlString) ?: return
        val videoFileName: String = getVideoFileName(urlString, videoFile)

        var u: URL? = null
        var inputStream: InputStream? = null

        try {
            u = URL(videoFile)
            inputStream = u.openStream()
            val huc = u.openConnection() as HttpURLConnection //to know the size of video
            val size = huc.getContentLength()

            val storagePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString()
            val f = File(storagePath, videoFileName)
            Log.e(TAG, "downloading to " + f.absolutePath);

            val fos = FileOutputStream(f)
            val buffer = ByteArray(1024)
            if (inputStream != null) {
                var len1 = inputStream.read(buffer)
                var progress = len1
                while (len1 > 0) {
                    fos.write(buffer, 0, len1)
                    len1 = inputStream.read(buffer)
                    progress += len1
                    listener.onProgress(progress.toFloat() / size.toFloat())
                }
            }
            listener.downloadFinished(f)

            fos.close()

        } catch (mue: MalformedURLException) {
            mue.printStackTrace()
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close()
                }
            } catch (ioe: IOException) {
                // just going to ignore this one
            }
        }


    }

    override fun isValid(urlString: String) : Boolean {
        val isFileType: Boolean = urlString.contains("www.rtp.pt/play")

        if (isFileType) {

            val videoFile: String? = getVideoFile(urlString);

            return videoFile != null
        }

        return false
    }

    private fun getVideoFile(urlString: String): String? {
        val doc : Document? = Jsoup.connect(urlString).timeout(10000).get();

        val scriptElements = doc?.getElementsByTag("script")

        if (scriptElements != null) {

            for (scriptElement in scriptElements.iterator()) {

                for (dataNode : DataNode in scriptElement.dataNodes()) {
                    if (dataNode.wholeData.contains("RTPPlayer")) {

                        val scriptText : String = dataNode.wholeData

                        try {

                            val rtpPlayerSubString: String = scriptText.substring(indexOfEx(scriptText, "RTPPlayer({"), scriptText.lastIndexOf("})"));

                            if (rtpPlayerSubString.indexOf(".mp4") >= 0) {  // is video file

                                if (rtpPlayerSubString.indexOf("fileKey: \"") >= 0) {

                                    var link = rtpPlayerSubString.substring(
                                            indexOfEx(rtpPlayerSubString, "fileKey: \""),
                                            indexOfEx(rtpPlayerSubString, "fileKey: \"") + rtpPlayerSubString.substring(indexOfEx(rtpPlayerSubString, "fileKey: \"")).indexOf("\","));


                                    return "http://cdn-ondemand.rtp.pt" + link;
                                }

                            } else if (rtpPlayerSubString.indexOf(".mp3") >= 0) { // is audio file

                                if (rtpPlayerSubString.indexOf("file: \"") >= 0) {

                                    return rtpPlayerSubString.substring(
                                            indexOfEx(rtpPlayerSubString, "file: \""),
                                            indexOfEx(rtpPlayerSubString, "file: \"") +rtpPlayerSubString.substring(indexOfEx(rtpPlayerSubString, "file: \"")).indexOf("\","));

                                }
                            }
                        } catch (parsingException : java.lang.Exception) {

                        }
                    }
                }
            }
        }

        return null;
    }

    private fun getVideoFileName(urlString: String, videoFile: String): String {
        val doc : Document? = Jsoup.connect(urlString).timeout(10000).get();

        val titleElements = doc?.getElementsByTag("title")

        if (titleElements != null && titleElements.size > 0) {

            var title = titleElements.elementAt(0).text();

            title = title.replace('-',' ')
                    .replace(':',' ')
                    .replace("\\s{2,}".toRegex(), " ")
                    .trim()
                    .replace(' ', '.')
                    .replace(' ', '.')
                    .replace(".RTP.Play.RTP", "")
            title = Normalizer.normalize(title, Normalizer.Form.NFKD );
            title = title.replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "");

            if (videoFile.indexOf(".mp4") >= 0) {  // is video file

                return title + ".mp4";

            } else if (videoFile.indexOf(".mp3") >= 0) { // is audio file

                return title + ".mp3";
            }

            return title;
        }

        return videoFile;
    }

    fun indexOfEx(string : String, subString: String) : Int {
        if (string.contains(subString)) {
            return string.indexOf(subString) + subString.length
        }
        return 0;
    }

}