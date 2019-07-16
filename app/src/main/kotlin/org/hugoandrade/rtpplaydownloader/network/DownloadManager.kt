package org.hugoandrade.rtpplaydownloader.network

import android.util.Log
import org.hugoandrade.rtpplaydownloader.DevConstants
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderMultiPartTaskBase
import android.content.Context
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTaskBase
import org.hugoandrade.rtpplaydownloader.network.download.FileIdentifier
import org.hugoandrade.rtpplaydownloader.network.parsing.ParseFuture
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingData
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationIdentifier
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationParseFuture
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationParserTaskBase
import org.hugoandrade.rtpplaydownloader.utils.FutureCallback
import org.hugoandrade.rtpplaydownloader.network.persistance.DatabaseModel
import org.hugoandrade.rtpplaydownloader.network.persistance.DownloadableEntry
import org.hugoandrade.rtpplaydownloader.network.persistance.DownloadableEntryParser
import org.hugoandrade.rtpplaydownloader.network.persistance.PersistencePresenterOps
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

    private lateinit var mDatabaseModel: DatabaseModel

    fun onCreate(viewOps: DownloadManagerViewOps) {
        mViewOps = WeakReference(viewOps)

        mDatabaseModel = object : DatabaseModel(){}
        mDatabaseModel.onCreate(object : PersistencePresenterOps {

            override fun onGetAllDownloadableEntries(downloadableEntries: List<DownloadableEntry>) {
                mViewOps.get()?.populateDownloadableItemsRecyclerView(DownloadableEntryParser.formatCollection(downloadableEntries))
            }

            override fun onDeleteAllDownloadableEntries(downloadableEntries: List<DownloadableEntry>) {
                mViewOps.get()?.populateDownloadableItemsRecyclerView(ArrayList())
            }

            override fun onResetDatabase(wasSuccessfullyDeleted : Boolean){
                mViewOps.get()?.populateDownloadableItemsRecyclerView(ArrayList())
            }

            override fun getActivityContext(): Context {
                return mViewOps.get()?.getActivityContext()!!
            }

            override fun getApplicationContext(): Context {
                return mViewOps.get()?.getApplicationContext()!!
            }
        })
    }

    fun onConfigurationChanged(viewOps: DownloadManagerViewOps) {
        mViewOps = WeakReference(viewOps)
    }

    fun onDestroy() {
        parsingExecutors.shutdownNow()
        downloadExecutors.shutdownNow()
        mDatabaseModel.onDestroy()
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

                val isPaginationUrlTheSameOfTheOriginalUrl : Boolean =
                        isPaginationUrlTheSameOfTheOriginalUrl(urlString, downloaderTask, paginationTask)

                paginationTask?.setPaginationComplete(isPaginationUrlTheSameOfTheOriginalUrl)

                if (downloaderTask is DownloaderMultiPartTaskBase) {
                    future.success(ParsingData(downloaderTask.tasks, paginationTask))
                }
                else {
                    future.success(ParsingData(downloaderTask, paginationTask))
                }
            }
        })

        return future
    }

    private fun isPaginationUrlTheSameOfTheOriginalUrl(urlString: String,
                                                       downloaderTask: DownloaderTaskBase,
                                                       paginationTask: PaginationParserTaskBase?): Boolean {
        Log.e(TAG, "isPaginationUrlTheSameOfTheOriginalUrl::")

        if (paginationTask == null) {
            return false
        }

        val paginationUrls : ArrayList<String> = paginationTask.parsePagination(urlString)

        Log.e(TAG, "isPaginationUrlTheSameOfTheOriginalUrl::0")
        if (paginationUrls.size == 0) {
            return false
        }

        val tasks : ArrayList<DownloaderTaskBase> = if (downloaderTask is DownloaderMultiPartTaskBase) {
            downloaderTask.tasks
        }
        else {
            arrayListOf(downloaderTask)
        }

        Log.e(TAG, "isPaginationUrlTheSameOfTheOriginalUrl::1::" + paginationUrls.toString())
        if (paginationUrls.size != 1) {
            return false
        }

        val paginationVideoFileNames : ArrayList<String> = ArrayList()
        val videoFileNames : ArrayList<String> = ArrayList()
        tasks.forEach(action = { task ->
            val videoFile = task.videoFile ?: return false
            videoFileNames.add(videoFile)
        })

        Log.e(TAG, "isPaginationUrlTheSameOfTheOriginalUrl::2")
        paginationUrls.forEach(action = { p ->

            if (!NetworkUtils.isNetworkAvailable(checkNotNull(mViewOps.get()).getApplicationContext())) {
                return false
            }

            val isUrl : Boolean = NetworkUtils.isValidURL(p)

            if (!isUrl) {
                return false
            }

            val paginationDownloaderTask: DownloaderTaskBase = FileIdentifier.findHost(p) ?: return false

            val parsing : Boolean = paginationDownloaderTask.parseMediaFile(p)

            if (!parsing) {
                return false
            }

            if (paginationDownloaderTask is DownloaderMultiPartTaskBase) {
                paginationDownloaderTask.tasks.forEach { t ->
                    val paginationVideoFileName = t.videoFile ?: return false
                    paginationVideoFileNames.add(paginationVideoFileName)
                }
            }
            else {
                val paginationVideoFileName = paginationDownloaderTask.videoFile ?: return false
                paginationVideoFileNames.add(paginationVideoFileName)
            }
        })

        return areEqual(videoFileNames, paginationVideoFileNames)
    }

    private fun areEqual(array0: ArrayList<String>, array1: ArrayList<String>): Boolean {
        Log.e(TAG, "areEqual::" + array0.toString())
        Log.e(TAG, "areEqual::" + array1.toString())
        if (array0.size != array1.size) {
            return false
        }

        array0.forEach { item0 ->
            var contains = false
            array1.forEach { item1 ->
                if (item0 == item1) contains = true
            }
            if (!contains) {
                return false
            }
        }
        return true
    }

    fun download(task: DownloaderTaskBase) : DownloadableItem  {
        val downloadableItem = DownloadableItem(task, mViewOps.get())
        downloadableItem.addDownloadStateChangeListener(object :DownloadableItemStateChangeListener {
            override fun onDownloadStateChange(downloadableItem: DownloadableItem) {
                // TODO
                if (downloadableItem.state == DownloadableItemState.Start) {
                    mDatabaseModel.insertDownloadableEntry(downloadableItem)
                }
                else {
                    mDatabaseModel.updateDownloadableEntry(downloadableItem)
                }
            }
        })
        return downloadableItem.startDownload()
    }

    fun retrieveItemsFromDB() {
        mDatabaseModel.retrieveAllDownloadableEntries()
    }

    fun emptyDB() {
        mDatabaseModel.deleteAllDownloadableEntries()
    }

    fun archive(downloadableItem: DownloadableItem) {
        downloadableItem.isArchived = true
        mDatabaseModel.updateDownloadableEntry(downloadableItem)
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
