package org.hugoandrade.rtpplaydownloader.network

import android.support.v4.util.ArrayMap
import android.util.Log
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderMultiPartTaskBase
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTaskBase
import org.hugoandrade.rtpplaydownloader.network.download.FileIdentifier
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationParserTaskBase
import org.hugoandrade.rtpplaydownloader.network.parsing.ParseFuture
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingData
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationIdentifier
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationParseFuture
import org.hugoandrade.rtpplaydownloader.utils.FutureCallback
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

                if (!parsing) {
                    future.failed("could not find filetype")
                    return
                }

                val paginationTask : PaginationParserTaskBase? = PaginationIdentifier.findHost(urlString)

                if (downloaderTask is DownloaderMultiPartTaskBase) {
                    future.success(ParsingData(
                            downloaderTask.tasks,
                            paginationTask))
                }
                else {
                    future.success(ParsingData(downloaderTask, paginationTask))
                }
            }
        }.start()

        return future
    }

    fun parsePagination(urlString: String, paginationTask: PaginationParserTaskBase): PaginationParseFuture {

        val future = PaginationParseFuture(urlString, paginationTask)

        object : Thread("Pagination Parsing Thread") {

            override fun run() {

                if (!NetworkUtils.isNetworkAvailable(checkNotNull(mViewOps.get()).getApplicationContext())) {
                    future.failed("no network")
                    return
                }

                val paginationUrls = paginationTask.parsePagination(urlString)
                val paginationUrlsProcessed = ArrayList<String>()
                val paginationDownloadTasks = ArrayMap<Double, DownloaderTaskBase>()

                paginationUrls.forEach(action = { paginationUrl ->
                    val paginationFuture : ParseFuture = parseUrl(paginationUrl)
                    paginationFuture.addCallback(object : FutureCallback<ParsingData> {

                        override fun onSuccess(result: ParsingData) {
                            Log.e(TAG, "pagination :: onSuccess")
                            for (task : DownloaderTaskBase in result.tasks) {
                                Log.e(TAG, "pagination :: " + task.videoFileName)
                                // unique index
                                paginationDownloadTasks[paginationUrls.indexOf(paginationUrl)
                                        + (result.tasks.indexOf(task) * 0.1 / result.tasks.size)] = task
                            }
                            fireCallbackIfNeeded(paginationUrl)
                        }

                        override fun onFailed(errorMessage: String) {
                            Log.d(TAG, "failed to download: $errorMessage")
                            fireCallbackIfNeeded(paginationUrl)
                        }

                        private fun fireCallbackIfNeeded(paginationUrl: String) {
                            synchronized(paginationUrls) {
                                paginationUrlsProcessed.add(paginationUrl)

                                if (paginationUrlsProcessed.size == paginationUrls.size) {
                                    if (paginationDownloadTasks.size == 0) {
                                        future.failed("no pagination urls found")
                                    }
                                    else {
                                        future.success(ArrayList(paginationDownloadTasks.values))
                                    }
                                }
                            }
                        }
                    })
                })
            }
        }.start()

        return future
    }

    fun download(task: DownloaderTaskBase) : DownloadableItem  {
        return DownloadableItem(task, mViewOps.get()).startDownload()
    }
}
