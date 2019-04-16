package org.hugoandrade.rtpplaydownloader.network

import org.hugoandrade.rtpplaydownloader.network.parsing.FileIdentifier
import org.hugoandrade.rtpplaydownloader.network.parsing.ParseFuture
import java.lang.ref.WeakReference

class DownloadManager  {

    /**
     * Debugging tag used by the Android logger.
     */
    protected var TAG = javaClass.simpleName

    private lateinit var mViewOps: WeakReference<DownloadManagerViewOps>

    fun onCreate(viewOps: DownloadManagerViewOps) {
        mViewOps = WeakReference(viewOps)
    }

    fun onDestroy() {
    }

    fun parseUrl(urlString: String) : ParseFuture {

        val future = ParseFuture(urlString)

        object : Thread() {

            override fun run() {

                val isUrl : Boolean = NetworkUtils.isValidURL(urlString)

                if (!isUrl) {
                    future.failed("is not a valid website")
                    return
                }

                val downloaderTask: DownloaderTaskBase? = FileIdentifier.findHost(urlString)

                if (downloaderTask == null) {
                    future.failed("is not a valid website")
                    return
                }

                val parsing : Boolean = downloaderTask.parseMediaFile(urlString)

                if (parsing) {
                    future.success(downloaderTask)
                    return
                }
                else {
                    future.failed("could not find filetype")
                }
            }
        }.start()

        return future
    }

    fun start(urlText: String) : DownloadableItem {

        return DownloadableItem(urlText, mViewOps.get()).start()
    }
}
