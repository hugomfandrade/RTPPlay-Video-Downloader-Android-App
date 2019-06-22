package org.hugoandrade.rtpplaydownloader.network

import android.util.Log
import org.hugoandrade.rtpplaydownloader.DevConstants
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderMultiPartTaskBase
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTaskBase
import org.hugoandrade.rtpplaydownloader.network.download.FileIdentifier
import org.hugoandrade.rtpplaydownloader.network.parsing.ParseFuture
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingData
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationIdentifier
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationParseFuture
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationParserTaskBase
import org.hugoandrade.rtpplaydownloader.utils.FutureCallback
import org.hugoandrade.rtpplaydownloader.utils.NetworkUtils
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

class DownloadManager  {

    /**
     * Debugging tag used by the Android logger.
     */
    @Suppress("PrivatePropertyName", "unused")
    private val TAG = javaClass.simpleName

    private lateinit var mViewOps: WeakReference<DownloadManagerViewOps>

    private val parsingExecutors = Executors.newFixedThreadPool(DevConstants.nParsingThreads)
    private val downloadExecutors = Executors.newFixedThreadPool(DevConstants.nDownloadThreads)

    fun onCreate(viewOps: DownloadManagerViewOps) {
        mViewOps = WeakReference(viewOps)
    }

    fun onDestroy() {
        parsingExecutors.shutdownNow()
        downloadExecutors.shutdownNow()
    }

    fun parseUrl(urlString: String) : ParseFuture {

        val future = ParseFuture(urlString)

        parsingExecutors.execute(object : Runnable {

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
        })

        return future
    }

    fun download(task: DownloaderTaskBase) : DownloadableItem  {
        val downloadableItem = DownloadableItem(task, mViewOps.get(), downloadExecutors)
        downloadableItem.startDownload()
        return downloadableItem
    }

    fun parsePagination(urlString: String, paginationTask: PaginationParserTaskBase): PaginationParseFuture {

        val future = PaginationParseFuture(urlString, paginationTask)

        parsingExecutors.execute(object : Runnable {

            override fun run() {

                if (!NetworkUtils.isNetworkAvailable(checkNotNull(mViewOps.get()).getApplicationContext())) {
                    future.failed("no network")
                    return
                }

                val paginationUrls = paginationTask.parsePagination(urlString)
                val paginationUrlsProcessed = ArrayList<String>()
                val paginationDownloadTasks = TreeMap<Double, DownloaderTaskBase>()

                if (paginationUrls.size == 0) {
                    future.success(ArrayList(paginationDownloadTasks.values))
                    return
                }

                paginationUrls.forEach(action = { paginationUrl ->
                    val paginationFuture : ParseFuture = parseUrl(paginationUrl)
                    paginationFuture.addCallback(object : FutureCallback<ParsingData> {

                        override fun onSuccess(result: ParsingData) {

                            for (task : DownloaderTaskBase in result.tasks) {
                                val i : Double = uniqueIndex(paginationUrls, paginationUrl, result.tasks, task)
                                // Log.e(TAG, "pagination :: " + task.videoFileName + "::" + i)
                                // unique index
                                paginationDownloadTasks[i] = task
                            }
                            fireCallbackIfNeeded(paginationUrl)
                        }

                        override fun onFailed(errorMessage: String) {
                            Log.d(TAG, "failed to download: $errorMessage")
                            fireCallbackIfNeeded(paginationUrl)
                        }

                        private fun uniqueIndex(paginationUrls: ArrayList<String>,
                                                paginationUrl: String,
                                                tasks: ArrayList<DownloaderTaskBase>,
                                                task: DownloaderTaskBase): Double {
                            return paginationUrls.indexOf(paginationUrl) + ((tasks.indexOf(task) + 1) * 0.1 / tasks.size)
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
        })

        return future
    }

    fun parseMore(urlString : String, paginationTask: PaginationParserTaskBase): PaginationParseFuture {
        val future = PaginationParseFuture(urlString, paginationTask)

        parsingExecutors.execute(object : Runnable {

            override fun run() {

                if (!NetworkUtils.isNetworkAvailable(checkNotNull(mViewOps.get()).getApplicationContext())) {
                    future.failed("no network")
                    return
                }

                val paginationUrls = paginationTask.parseMore()
                val paginationUrlsProcessed = ArrayList<String>()
                val paginationDownloadTasks = TreeMap<Double, DownloaderTaskBase>()

                if (paginationUrls.size == 0) {
                    future.success(ArrayList(paginationDownloadTasks.values))
                    return
                }

                paginationUrls.forEach(action = { paginationUrl ->
                    val paginationFuture : ParseFuture = parseUrl(paginationUrl)
                    paginationFuture.addCallback(object : FutureCallback<ParsingData> {

                        override fun onSuccess(result: ParsingData) {

                            for (task : DownloaderTaskBase in result.tasks) {
                                val i : Double = uniqueIndex(paginationUrls, paginationUrl, result.tasks, task)
                                // Log.e(TAG, "pagination :: " + task.videoFileName + "::" + i)
                                // unique index
                                paginationDownloadTasks[i] = task
                            }
                            fireCallbackIfNeeded(paginationUrl)
                        }

                        private fun uniqueIndex(paginationUrls: ArrayList<String>,
                                                paginationUrl: String,
                                                tasks: ArrayList<DownloaderTaskBase>,
                                                task: DownloaderTaskBase): Double {
                            return paginationUrls.indexOf(paginationUrl) + ((tasks.indexOf(task) + 1) * 0.1 / tasks.size)
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
        })

        return future
    }
}
