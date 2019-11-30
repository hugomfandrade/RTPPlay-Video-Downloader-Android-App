package org.hugoandrade.rtpplaydownloader.network

import android.util.Log
import org.hugoandrade.rtpplaydownloader.DevConstants
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderMultiPartTaskBase
import android.content.Context
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTaskBase
import org.hugoandrade.rtpplaydownloader.network.download.EmptyDownloaderTask
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
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class DownloadManager : IDownloadManager {

    /**
     * Debugging tag used by the Android logger.
     */
    @Suppress("PrivatePropertyName", "unused")
    private val TAG = javaClass.simpleName

    private val downloadableItemList: java.util.ArrayList<DownloadableItemAction> = java.util.ArrayList()

    private lateinit var mViewOps: WeakReference<DownloadManagerViewOps>

    private val parsingExecutors = Executors.newFixedThreadPool(DevConstants.nParsingThreads)
    private val downloadExecutors = Executors.newFixedThreadPool(DevConstants.nDownloadThreads)

    private lateinit var mDatabaseModel: DatabaseModel

    override fun onCreate(viewOps: DownloadManagerViewOps) {
        mViewOps = WeakReference(viewOps)

        mDatabaseModel = object : DatabaseModel(){}
        mDatabaseModel.onCreate(object : PersistencePresenterOps {

            override fun onInsertDownloadableEntry(downloadableEntry: DownloadableEntry) {
                if (!DevConstants.enablePersistence) return

                val item : DownloadableItem = DownloadableEntryParser.formatSingleton(downloadableEntry)

                val filename = item.mediaFileName ?: return
                val task = persistenceMap[filename] ?: return

                val action = DownloadableItemAction(item, task, downloadExecutors)
                item.addDownloadStateChangeListener(object :DownloadableItemStateChangeListener {
                    override fun onDownloadStateChange(downloadableItem: DownloadableItem) {
                        mDatabaseModel.updateDownloadableEntry(downloadableItem)
                    }
                })
                action.startDownload()

                downloadableItemList.add(action)
                downloadableItemList.sortedWith(compareBy { it.item.id } )
                mViewOps.get()?.displayDownloadableItem(action)
            }

            override fun onGetAllDownloadableEntries(downloadableEntries: List<DownloadableEntry>) {
                if (!DevConstants.enablePersistence) return

                val items : List<DownloadableItem> = DownloadableEntryParser.formatCollection(downloadableEntries)

                val actions : ArrayList<DownloadableItemAction> = ArrayList()

                items.forEach{item ->
                    run {

                        val url= item.url
                        val task : DownloaderTaskBase = FileIdentifier.findHostForSingleTask(url) ?: EmptyDownloaderTask()
                        task.url = item.url
                        task.mediaUrl = item.filepath
                        task.thumbnailUrl = item.thumbnailUrl
                        task.mediaFileName = item.mediaFileName
                        task.isDownloading = false
                        val action = DownloadableItemAction(item, task, downloadExecutors)
                        item.addDownloadStateChangeListener(object : DownloadableItemStateChangeListener {

                            override fun onDownloadStateChange(downloadableItem: DownloadableItem) {
                                if (!DevConstants.enablePersistence) return

                                mDatabaseModel.updateDownloadableEntry(downloadableItem)
                            }
                        })
                        actions.add(action)
                    }
                }

                downloadableItemList.addAll(actions)
                downloadableItemList.sortedWith(compareBy { it.item.id } )
                mViewOps.get()?.displayDownloadableItems(actions)
            }

            override fun onDeleteAllDownloadableEntries(downloadableEntries: List<DownloadableEntry>) {
                if (!DevConstants.enablePersistence) return

                downloadableItemList.clear()
                mViewOps.get()?.displayDownloadableItems(ArrayList())
            }

            override fun onResetDatabase(wasSuccessfullyDeleted : Boolean){
                if (!DevConstants.enablePersistence) return

                downloadableItemList.clear()
                mViewOps.get()?.displayDownloadableItems(ArrayList())
            }

            override fun getActivityContext(): Context? {
                return mViewOps.get()?.getActivityContext()
            }

            override fun getApplicationContext(): Context? {
                return mViewOps.get()?.getApplicationContext()
            }
        })
    }

    override fun onConfigurationChanged(viewOps: DownloadManagerViewOps) {
        mViewOps = WeakReference(viewOps)

        mViewOps.get()?.displayDownloadableItems(downloadableItemList)
    }

    override fun onDestroy() {
        parsingExecutors.shutdownNow()
        downloadExecutors.shutdownNow()
        mDatabaseModel.onDestroy()
    }

    override fun parseUrl(url: String) : ParseFuture {

        val future = ParseFuture(url)

        parsingExecutors.execute(object : Runnable {

            override fun run() {

                if (!NetworkUtils.isNetworkAvailable(checkNotNull(mViewOps.get()).getApplicationContext())) {
                    future.failed("no network")
                    return
                }

                val isUrl : Boolean = NetworkUtils.isValidURL(url)

                if (!isUrl) {
                    future.failed("is not a valid website")
                    return
                }

                val downloaderTask: DownloaderTaskBase? = FileIdentifier.findHost(url)
                val paginationTask : PaginationParserTaskBase? = PaginationIdentifier.findHost(url)

                if (downloaderTask == null && paginationTask != null) {

                    try {
                        val f : ArrayList<DownloaderTaskBase>? = parsePagination(url, paginationTask).get()
                        if (f != null) {
                            future.success(ParsingData(f, paginationTask))
                        }
                        else {
                            future.failed("is not a valid website")
                        }
                    }
                    catch (e : ExecutionException) {
                        future.failed("is not a valid website")
                    }

                    return
                }

                if (downloaderTask == null) {
                    future.failed("is not a valid website")
                    return
                }

                val parsing : Boolean = downloaderTask.parseMediaFile(url)

                if (!parsing) {
                    future.failed("could not find filetype")
                    return
                }

                val isPaginationUrlTheSameOfTheOriginalUrl : Boolean =
                        isPaginationUrlTheSameOfTheOriginalUrl(url, downloaderTask, paginationTask)

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

    override fun parsePagination(url: String, paginationTask: PaginationParserTaskBase): PaginationParseFuture {

        val future = PaginationParseFuture(url, paginationTask)

        parsingExecutors.execute(object : Runnable {

            override fun run() {

                if (!NetworkUtils.isNetworkAvailable(checkNotNull(mViewOps.get()).getApplicationContext())) {
                    future.failed("no network")
                    return
                }

                val paginationUrls = paginationTask.parsePagination(url)
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
                                // Log.e(TAG, "pagination :: " + task.mediaFileName + "::" + i)
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

    override fun parseMore(url : String, paginationTask: PaginationParserTaskBase): PaginationParseFuture {
        val future = PaginationParseFuture(url, paginationTask)

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
                                // Log.e(TAG, "pagination :: " + task.mediaFileName + "::" + i)
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

    private fun isPaginationUrlTheSameOfTheOriginalUrl(url: String,
                                                       downloaderTask: DownloaderTaskBase,
                                                       paginationTask: PaginationParserTaskBase?): Boolean {

        if (paginationTask == null) {
            return false
        }

        val paginationUrls : ArrayList<String> = paginationTask.parsePagination(url)

        if (paginationUrls.size == 0) {
            return false
        }

        val tasks : ArrayList<DownloaderTaskBase> = if (downloaderTask is DownloaderMultiPartTaskBase) {
            downloaderTask.tasks
        }
        else {
            arrayListOf(downloaderTask)
        }

        if (paginationUrls.size != 1) {
            return false
        }

        val paginationVideoFileNames : ArrayList<String> = ArrayList()
        val videoFileNames : ArrayList<String> = ArrayList()
        tasks.forEach(action = { task ->
            val videoFile = task.mediaUrl ?: return false
            videoFileNames.add(videoFile)
        })

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
                    val paginationVideoFileName = t.mediaUrl ?: return false
                    paginationVideoFileNames.add(paginationVideoFileName)
                }
            }
            else {
                val paginationVideoFileName = paginationDownloaderTask.mediaUrl ?: return false
                paginationVideoFileNames.add(paginationVideoFileName)
            }
        })

        return areEqual(videoFileNames, paginationVideoFileNames)
    }

    private fun areEqual(array0: ArrayList<String>, array1: ArrayList<String>): Boolean {
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

    override fun download(task: DownloaderTaskBase)  {
        val url = task.url?: return
        val mediaFileName = task.mediaFileName?: return
        val thumbnailUrl = task.thumbnailUrl

        val item = DownloadableItem(url, mediaFileName, thumbnailUrl)

        if (!DevConstants.enablePersistence) {
            val action = DownloadableItemAction(item, task, downloadExecutors)
            action.startDownload()

            downloadableItemList.add(action)
            downloadableItemList.sortedWith(compareBy { it.item.id } )
            mViewOps.get()?.displayDownloadableItem(action)
            return
        }

        persistenceMap[item.mediaFileName] = task

        mDatabaseModel.insertDownloadableEntry(item)
    }

    private val persistenceMap : HashMap<String, DownloaderTaskBase> = HashMap()

    override fun retrieveItemsFromDB() {
        if (!DevConstants.enablePersistence) return
        mDatabaseModel.retrieveAllDownloadableEntries()
    }

    override fun emptyDB() {
        if (!DevConstants.enablePersistence) return
        mDatabaseModel.deleteAllDownloadableEntries()
    }

    override fun archive(downloadableItem: DownloadableItem) {
        if (!DevConstants.enablePersistence) return
        downloadableItem.isArchived = true
        mDatabaseModel.updateDownloadableEntry(downloadableItem)
    }
}
