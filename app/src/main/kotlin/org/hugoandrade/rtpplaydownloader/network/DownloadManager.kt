package org.hugoandrade.rtpplaydownloader.network

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.support.v4.content.ContextCompat
import android.util.Log
import org.hugoandrade.rtpplaydownloader.DevConstants
import org.hugoandrade.rtpplaydownloader.R
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTask
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.ParsingMultiPartTaskBase
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.ParsingTaskBase
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.ParsingIdentifier
import org.hugoandrade.rtpplaydownloader.network.parsing.ParseFuture
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingData
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationIdentifier
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationParseFuture
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationParserTaskBase
import org.hugoandrade.rtpplaydownloader.network.persistance.DatabaseModel
import org.hugoandrade.rtpplaydownloader.network.persistance.PersistencePresenterOps
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.hugoandrade.rtpplaydownloader.utils.FutureCallback
import org.hugoandrade.rtpplaydownloader.utils.NetworkUtils
import org.hugoandrade.rtpplaydownloader.utils.ViewUtils
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList


class DownloadManager : IDownloadManager {
    /**
     * Debugging tag used by the Android logger.
     */
    @Suppress("PrivatePropertyName", "unused")
    private val TAG = javaClass.simpleName

    private val downloadableItems: ArrayList<DownloadableItemAction> = ArrayList()

    private lateinit var mViewOps: WeakReference<DownloadManagerViewOps>

    private val parsingExecutors = Executors.newFixedThreadPool(DevConstants.nParsingThreads)

    private lateinit var mDatabaseModel: DatabaseModel

    private var downloadService: DownloadService.DownloadServiceBinder? = null

    override fun onCreate(viewOps: DownloadManagerViewOps) {
        mViewOps = WeakReference(viewOps)

        startService()

        mDatabaseModel = object : DatabaseModel(){}
        mDatabaseModel.onCreate(object : PersistencePresenterOps {

            override fun onDownloadableItemInserted(downloadableItem: DownloadableItem?) {
                if (!DevConstants.enablePersistence) return
                if (downloadableItem == null) {
                    Log.e(TAG, "failed to insert")
                    return
                }

                val mediaUrl = downloadableItem.mediaUrl ?: return
                val filename = downloadableItem.filename ?: return

                val downloaderTask = DownloaderTask(mediaUrl, filename, downloadableItem)
                val action = DownloadableItemAction(downloadableItem, downloaderTask)
                action.addActionListener(actionListener)
                downloadableItem.addDownloadStateChangeListener(object :DownloadableItemState.ChangeListener {
                    override fun onDownloadStateChange(downloadableItem: DownloadableItem) {
                        mDatabaseModel.updateDownloadableEntry(downloadableItem)
                    }
                })

                downloadService?.startDownload(action)

                downloadableItems.add(action)
                downloadableItems.sortedWith(compareBy { it.item.id } )
                mViewOps.get()?.displayDownloadableItem(action)
            }

            override fun onDownloadableItemsRetrieved(downloadableItems: List<DownloadableItem>) {
                if (!DevConstants.enablePersistence) return

                val actions : ArrayList<DownloadableItemAction> = ArrayList()

                downloadableItems.forEach{ item ->
                    run {

                        item.addDownloadStateChangeListener(object : DownloadableItemState.ChangeListener {

                            override fun onDownloadStateChange(downloadableItem: DownloadableItem) {
                                if (!DevConstants.enablePersistence) return

                                mDatabaseModel.updateDownloadableEntry(downloadableItem)
                            }
                        })

                        val task = DownloaderTask(item.mediaUrl?:"", item.filename?:"", item)
                        val action = DownloadableItemAction(item, task)
                        action.addActionListener(actionListener)
                        actions.add(action)

                        var isPresent = false
                        for (i in this@DownloadManager.downloadableItems.size - 1 downTo 0) {
                            if (this@DownloadManager.downloadableItems[i].item.id == action.item.id) {
                                isPresent = true
                                break
                            }
                        }

                        if (!isPresent) {
                            if (action.item.state == DownloadableItemState.Downloading) {
                                action.item.state = DownloadableItemState.Failed
                            }
                            this@DownloadManager.downloadableItems.add(action)
                            this@DownloadManager.downloadableItems.sortedWith(compareBy { it.item.id } )
                            mViewOps.get()?.displayDownloadableItem(action)
                        }
                    }
                }

                this@DownloadManager.downloadableItems.sortedWith(compareBy { it.item.id } )
                mViewOps.get()?.displayDownloadableItems(this@DownloadManager.downloadableItems)
            }

            override fun onDownloadableItemsDeleted(downloadableItems: List<DownloadableItem>) {
                if (!DevConstants.enablePersistence) return

                this@DownloadManager.downloadableItems.clear()
                mViewOps.get()?.displayDownloadableItems(ArrayList())
            }

            override fun onDatabaseReset(wasSuccessfullyDeleted : Boolean){
                if (!DevConstants.enablePersistence) return

                downloadableItems.clear()
                mViewOps.get()?.displayDownloadableItems(ArrayList())
            }

            override fun getActivityContext(): Context? {
                return mViewOps.get()?.getActivityContext()
            }

            override fun getApplicationContext(): Context? {
                return mViewOps.get()?.getApplicationContext()
            }
        })

        retrieveItemsFromDB()
    }

    override fun onConfigurationChanged(viewOps: DownloadManagerViewOps) {
        mViewOps = WeakReference(viewOps)

        mViewOps.get()?.displayDownloadableItems(downloadableItems)
    }

    override fun onDestroy() {
        parsingExecutors.shutdownNow()
        mDatabaseModel.onDestroy()

        stopService()
    }

    private fun startService() {
        val context = mViewOps.get()?.getApplicationContext() ?: return

        val serviceIntent = Intent(context, DownloadService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
        context.bindService(serviceIntent, mConnection, 0)
    }

    private fun stopService() {
        val context = mViewOps.get()?.getApplicationContext() ?: return
        context.unbindService(mConnection)
    }

    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            downloadService = (service as DownloadService.DownloadServiceBinder)

            val downloadService = this@DownloadManager.downloadService
            val activeItemActions : ArrayList<DownloadableItemAction> =  downloadService?.getItemActions()?:return

            for (itemAction in activeItemActions) {
                for (i in downloadableItems.size - 1 downTo 0) {
                    if (downloadableItems[i].item.id == itemAction.item.id) {
                        downloadableItems.removeAt(i)
                        downloadableItems.add(itemAction)
                        break
                    }
                }

                if (!downloadableItems.contains(itemAction)) {
                    downloadableItems.add(itemAction)
                }
            }
            downloadableItems.sortedWith(compareBy { it.item.id } )

            mViewOps.get()?.displayDownloadableItems(downloadableItems)
        }

        override fun onServiceDisconnected(className: ComponentName) {
            downloadService = null
        }
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

                val parsingTask: ParsingTaskBase? = ParsingIdentifier.findHost(url)
                val paginationTask : PaginationParserTaskBase? = PaginationIdentifier.findHost(url)

                if (parsingTask == null && paginationTask != null) {

                    try {
                        val f : ArrayList<ParsingTaskBase>? = parsePagination(url, paginationTask).get()
                        if (f != null) {
                            future.success(ParsingData(f, paginationTask))
                        }
                        else {
                            future.failed("is not a valid website")
                        }
                    }
                    catch (e : Exception) {
                        future.failed("is not a valid website")
                    }

                    return
                }

                if (parsingTask == null) {
                    future.failed("is not a valid website")
                    return
                }

                val parsing : Boolean = parsingTask.parseMediaFile(url)

                if (!parsing) {
                    future.failed("could not find filetype")
                    return
                }

                val isPaginationUrlTheSameOfTheOriginalUrl : Boolean =
                        isPaginationUrlTheSameOfTheOriginalUrl(url, parsingTask, paginationTask)

                paginationTask?.setPaginationComplete(isPaginationUrlTheSameOfTheOriginalUrl)

                if (parsingTask is ParsingMultiPartTaskBase) {
                    future.success(ParsingData(parsingTask.tasks, paginationTask))
                }
                else {
                    future.success(ParsingData(parsingTask, paginationTask))
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
                val paginationDownloadTasks = TreeMap<Double, ParsingTaskBase>()

                if (paginationUrls.size == 0) {
                    future.success(ArrayList(paginationDownloadTasks.values))
                    return
                }

                paginationUrls.forEach(action = { paginationUrl ->
                    val paginationFuture : ParseFuture = parseUrl(paginationUrl)
                    paginationFuture.addCallback(object : FutureCallback<ParsingData> {

                        override fun onSuccess(result: ParsingData) {

                            for (task : ParsingTaskBase in result.tasks) {
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
                                                tasks: ArrayList<ParsingTaskBase>,
                                                task: ParsingTaskBase): Double {
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
                val paginationDownloadTasks = TreeMap<Double, ParsingTaskBase>()

                if (paginationUrls.size == 0) {
                    future.success(ArrayList(paginationDownloadTasks.values))
                    return
                }

                paginationUrls.forEach(action = { paginationUrl ->
                    val paginationFuture : ParseFuture = parseUrl(paginationUrl)
                    paginationFuture.addCallback(object : FutureCallback<ParsingData> {

                        override fun onSuccess(result: ParsingData) {

                            for (task : ParsingTaskBase in result.tasks) {
                                val i : Double = uniqueIndex(paginationUrls, paginationUrl, result.tasks, task)
                                // Log.e(TAG, "pagination :: " + task.mediaFileName + "::" + i)
                                // unique index
                                paginationDownloadTasks[i] = task
                            }
                            fireCallbackIfNeeded(paginationUrl)
                        }

                        private fun uniqueIndex(paginationUrls: ArrayList<String>,
                                                paginationUrl: String,
                                                tasks: ArrayList<ParsingTaskBase>,
                                                task: ParsingTaskBase): Double {
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
                                                       parsingTask: ParsingTaskBase,
                                                       paginationTask: PaginationParserTaskBase?): Boolean {

        if (paginationTask == null) {
            return false
        }

        val paginationUrls : ArrayList<String> = paginationTask.parsePagination(url)

        if (paginationUrls.size == 0) {
            return false
        }

        val tasks : ArrayList<ParsingTaskBase> = if (parsingTask is ParsingMultiPartTaskBase) {
            parsingTask.tasks
        }
        else {
            arrayListOf(parsingTask)
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

            val paginationParsingTask: ParsingTaskBase = ParsingIdentifier.findHost(p) ?: return false

            val parsing : Boolean = paginationParsingTask.parseMediaFile(p)

            if (!parsing) {
                return false
            }

            if (paginationParsingTask is ParsingMultiPartTaskBase) {
                paginationParsingTask.tasks.forEach { t ->
                    val paginationVideoFileName = t.mediaUrl ?: return false
                    paginationVideoFileNames.add(paginationVideoFileName)
                }
            }
            else {
                val paginationVideoFileName = paginationParsingTask.mediaUrl ?: return false
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

    override fun download(task: ParsingTaskBase)  {
        val url = task.url?: return
        val mediaUrl = task.mediaUrl?: return
        val filename = task.filename?: return
        val thumbnailUrl = task.thumbnailUrl

        val item = DownloadableItem(url, mediaUrl, thumbnailUrl, filename)
        item.downloadTask = ParsingIdentifier.findType(task)?.name

        if (DevConstants.enablePersistence) {
            mDatabaseModel.insertDownloadableItem(item)
        }
        else {
            val downloadTask = DownloaderTask(mediaUrl, filename, item)
            val action = DownloadableItemAction(item, downloadTask)
            action.addActionListener(actionListener)
            downloadService?.startDownload(action)

            downloadableItems.add(action)
            downloadableItems.sortedWith(compareBy { it.item.id } )

            mViewOps.get()?.displayDownloadableItem(action)
        }
    }

    override fun retrieveItemsFromDB() {
        if (!DevConstants.enablePersistence) return
        mDatabaseModel.retrieveAllDownloadableItems()
    }

    override fun emptyDB() {
        if (!DevConstants.enablePersistence) return
        mDatabaseModel.deleteAllDownloadableItem()
    }

    override fun archive(downloadableItem: DownloadableItem) {
        if (!DevConstants.enablePersistence) return
        downloadableItem.isArchived = true
        mDatabaseModel.updateDownloadableEntry(downloadableItem)
    }

    private val actionListener: DownloadableItemActionListener = object : DownloadableItemActionListener {
        override fun onPlay(action: DownloadableItemAction) {
            try {
                val filepath = action.item.filepath
                if (MediaUtils.doesMediaFileExist(action.item)) {
                    mViewOps.get()?.getApplicationContext()?.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(filepath))
                                    .setDataAndType(Uri.parse(filepath), "video/mp4"))
                } else {
                    ViewUtils.showToast(
                            mViewOps.get()?.getActivityContext(),
                            mViewOps.get()?.getActivityContext()?.getString(R.string.file_not_found))
                }
            }catch (ignored : Exception) {}
        }

        override fun onRefresh(action: DownloadableItemAction) {
            action.cancel()
            MediaUtils.deleteMediaFileIfExist(action.item)
            downloadService?.startDownload(action)
        }
    }
}
