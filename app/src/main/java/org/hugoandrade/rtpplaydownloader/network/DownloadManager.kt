package org.hugoandrade.rtpplaydownloader.network

import android.util.Log
import java.lang.ref.WeakReference
import java.net.URL

class DownloadManager {

    /**
     * Debugging tag used by the Android logger.
     */
    protected var TAG = javaClass.simpleName

    private lateinit var mViewOps: WeakReference<DownloadManagerViewOps>

    private var mFileIdentifier : FileIdentifier = FileIdentifier();

    interface DownloadManagerViewOps {
        fun onParsingEnded(url: String, isOk: Boolean, message : String);
        fun runOnUiThread(runnable: Runnable)
    }

    fun onCreate(viewOps: DownloadManagerViewOps) {
        mViewOps = WeakReference(viewOps);
    }

    fun onDestroy() {
    }

    fun start(urlText: String) {

        val isUrl = isValidURL(urlText);

        if (!isUrl) {
            mViewOps.get()?.onParsingEnded(urlText, false, "is not a valid website");
            return
        }

        object : Thread() {
            override fun run() {

                val fileType: FileType? = mFileIdentifier.findHost(urlText);

                if (fileType == null) {
                    mViewOps.get()?.runOnUiThread(Runnable {
                        mViewOps.get()?.onParsingEnded(urlText, false, "could not find filetype");
                    })
                }
                else {
                    fileType.mDownloaderTask.downloadAsync(urlText)
                }
            }
        }.start()
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
