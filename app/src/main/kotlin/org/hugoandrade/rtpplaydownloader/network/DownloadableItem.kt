package org.hugoandrade.rtpplaydownloader.network

import android.util.Log
import java.io.File
import java.net.URL


class DownloadableItem(urlText: String, viewOps: DownloadManager.DownloadManagerViewOps?) : DownloaderTaskListener {

    var TAG : String = javaClass.simpleName

    private val urlText: String = urlText
    private val viewOps : DownloadManager.DownloadManagerViewOps? = viewOps

    private val mFileIdentifier : FileIdentifier = FileIdentifier()
    private var downloaderTask: DownloaderTaskBase? = null

    private val filename: String? = null
    private val filepath: String? = null

    fun start(): DownloadableItem {

        val isUrl = isValidURL(urlText);

        if (!isUrl) {
            viewOps?.onParsingEnded(urlText, false, "is not a valid website");
            return this
        }

        object : Thread() {
            override fun run() {

                downloaderTask = mFileIdentifier.findHost(urlText);

                downloaderTask?.downloadAsync(this@DownloadableItem, urlText) ?: viewOps?.onParsingEnded(urlText, false, "could not find filetype")
            }
        }.start()
        return this
    }

    override fun onProgress(progress: Float) {
        viewOps?.onDownloading(progress);
    }

    override fun downloadFinished(f: File) {
        Log.e(TAG, "finished downloading to " + f.absolutePath);
        viewOps?.onParsingEnded(f.absolutePath, true, "finished downloading");
    }

    private fun isValidURL(urlText: String): Boolean {
        try {
            val url = URL(urlText);
            return "http".equals(url.getProtocol()) || "https".equals(url.getProtocol());
        } catch (e : Exception) {
            return false;
        }
    }
}