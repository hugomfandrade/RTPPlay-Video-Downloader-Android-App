package org.hugoandrade.rtpplaydownloader.network

import android.content.Context
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTaskBase
import org.hugoandrade.rtpplaydownloader.network.parsing.FileIdentifier
import org.hugoandrade.rtpplaydownloader.network.parsing.ParseFuture
import org.hugoandrade.rtpplaydownloader.utils.NetworkUtils
import java.lang.ref.WeakReference

class DownloadManager  {

    /**
     * Debugging tag used by the Android logger.
     */
    @Suppress("PrivatePropertyName", "unused")
    private val TAG = javaClass.simpleName

    private lateinit var mViewOps: WeakReference<DownloadManagerViewOps>

    fun onCreate(viewOps: DownloadManagerViewOps) {
        mViewOps = WeakReference(viewOps)
    }

    fun onDestroy() {
    }

    fun parseUrl(urlString: String) : ParseFuture {

        val future = ParseFuture(urlString)

        object : Thread("Parsing Thread") {

            override fun run() {

                if (!NetworkUtils.isNetworkAvailable(checkNotNull(mViewOps.get()).getApplicationContext())) {
                    future.failed("no network")
                    return
                }

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

    fun download(task: DownloaderTaskBase) : DownloadableItem  {
        return DownloadableItem(task, mViewOps.get()).startDownload()
    }
}
