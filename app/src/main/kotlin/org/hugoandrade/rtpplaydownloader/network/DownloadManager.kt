package org.hugoandrade.rtpplaydownloader.network

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import org.hugoandrade.rtpplaydownloader.DevConstants
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderIdentifier
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTask
import org.hugoandrade.rtpplaydownloader.network.download.RTPPlayTSDownloaderTask
import org.hugoandrade.rtpplaydownloader.network.download.TVIPlayerTSDownloaderTask
import org.hugoandrade.rtpplaydownloader.network.parsing.ParseFuture
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingData
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationIdentifier
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationParseFuture
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationParserTaskBase
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.ParsingIdentifier
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.ParsingMultiPartTaskBase
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.ParsingTaskBase
import org.hugoandrade.rtpplaydownloader.network.persistence.DatabaseModel
import org.hugoandrade.rtpplaydownloader.network.persistence.PersistencePresenterOps
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.hugoandrade.rtpplaydownloader.utils.FutureCallback
import org.hugoandrade.rtpplaydownloader.utils.NetworkUtils
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

class DownloadManager(application: Application) : AndroidViewModel(application), DownloadManagerAPI {
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

    private val mPersistencePresenterOps = object : PersistencePresenterOps {

        override fun onDownloadableItemInserted(downloadableItem: DownloadableItem?) {
            if (downloadableItem == null) {
                Log.e(TAG, "failed to insert")
                return
            }

            val context : Application = getApplication()
            val url = downloadableItem.url
            val mediaUrl = downloadableItem.mediaUrl ?: return
            val filename = downloadableItem.filename ?: return
            val downloadTask = downloadableItem.downloadTask
            val dirPath = MediaUtils.getDownloadsDirectory(context)
            val dir = dirPath?.toString() ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString()

            val downloaderTask: DownloaderTask = when(DownloaderIdentifier.findHost(downloadTask, mediaUrl)) {
                DownloaderIdentifier.DownloadType.FullFile -> DownloaderTask(mediaUrl, dir, filename, downloadableItem)
                DownloaderIdentifier.DownloadType.TVITSFiles -> TVIPlayerTSDownloaderTask(url, mediaUrl, dir, filename, downloadableItem)
                DownloaderIdentifier.DownloadType.RTPTSFiles -> {
                    if (filename.endsWith(".mp3")) DownloaderTask(mediaUrl, dir, filename, downloadableItem)
                    else  RTPPlayTSDownloaderTask(url, mediaUrl, dir, filename, downloadableItem)
                }
                null -> return
            }

            val action = DownloadableItemAction(downloadableItem, downloaderTask)
            action.addActionListener(actionListener)

            downloadService?.startDownload(action)

            downloadableItems.add(action)
            downloadableItems.sortedWith(compareBy { it.item.id } )
            mViewOps.get()?.displayDownloadableItem(action)
        }

        override fun onDownloadableItemsRetrieved(downloadableItems: List<DownloadableItem>) {

            val actions : ArrayList<DownloadableItemAction> = ArrayList()

            val context : Application = getApplication()
            val dirPath = MediaUtils.getDownloadsDirectory(context)
            val dir = dirPath?.toString() ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString()

            for (item in downloadableItems) {

                synchronized(this@DownloadManager.downloadableItems) {
                    val listItems = this@DownloadManager.downloadableItems


                    var contains = false
                    // add if not already in list
                    for (i in listItems.size - 1 downTo 0) {
                        if (listItems[i].item.id == item.id) {
                            contains = true
                            break
                        }
                    }

                    if (!contains) {

                        val task: DownloaderTask? = when(DownloaderIdentifier.findHost(item.downloadTask, item.mediaUrl?: "")) {
                            DownloaderIdentifier.DownloadType.FullFile -> DownloaderTask(item.mediaUrl ?: "",  dir, item.filename ?: "", item)
                            DownloaderIdentifier.DownloadType.TVITSFiles -> TVIPlayerTSDownloaderTask(item.url, item.mediaUrl ?: "", dir, item.filename ?: "", item)
                            DownloaderIdentifier.DownloadType.RTPTSFiles ->  {
                                val mediaUrl = item.mediaUrl
                                if (mediaUrl != null && mediaUrl.endsWith(".mp3")) DownloaderTask(mediaUrl, dir, item.filename ?: "", item)
                                else  RTPPlayTSDownloaderTask(item.url, item.mediaUrl ?: "", dir, item.filename ?: "", item)
                            }
                            null -> null
                        }

                        if (task != null) {

                            val action = DownloadableItemAction(item, task)
                            action.addActionListener(actionListener)
                            actions.add(action)
                            if (action.item.state == DownloadableItem.State.Downloading) {
                                action.item.state = DownloadableItem.State.Failed
                            }
                            listItems.add(action)
                            listItems.sortedWith(compareBy { it.item.id })

                            mViewOps.get()?.displayDownloadableItem(action)
                        }
                    }
                }
            }

            mViewOps.get()?.displayDownloadableItems(this@DownloadManager.downloadableItems)
        }

        override fun onDownloadableItemsDeleted(downloadableItems: List<DownloadableItem>) {
            this@DownloadManager.downloadableItems.clear()
            mViewOps.get()?.displayDownloadableItems(this@DownloadManager.downloadableItems)
        }

        override fun onDatabaseReset(wasSuccessfullyDeleted : Boolean) {

            downloadableItems.clear()
            mViewOps.get()?.displayDownloadableItems(downloadableItems)
        }
    }

    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            downloadService = (service as DownloadService.DownloadServiceBinder)

            val downloadService = this@DownloadManager.downloadService
            val activeItemActions : ArrayList<DownloadableItemAction> =  downloadService?.getItemActions()?:return

            // populate with items which are being downloaded
            for (itemAction in activeItemActions) {

                synchronized(this@DownloadManager.downloadableItems) {
                    val listItems = this@DownloadManager.downloadableItems
                    for (i in listItems.size - 1 downTo 0) {
                        if (listItems[i].item.id == itemAction.item.id) {
                            listItems.removeAt(i)
                            break
                        }
                    }

                    listItems.add(itemAction)

                    listItems.sortedWith(compareBy { it.item.id })
                }
            }

            mViewOps.get()?.displayDownloadableItems(downloadableItems)
        }

        override fun onServiceDisconnected(className: ComponentName) {
            downloadService = null
        }
    }

    init {
        startService()

        mDatabaseModel = object : DatabaseModel(getApplication()){}
        mDatabaseModel.onCreate(mPersistencePresenterOps)

        retrieveItemsFromDB()
    }

    override fun attachCallback(viewOps: DownloadManagerViewOps) {
        mViewOps = WeakReference(viewOps)
    }

    override fun onCleared() {
        super.onCleared()

        parsingExecutors.shutdownNow()
        mDatabaseModel.onDestroy()

        stopService()
    }

    private fun startService() {
        val context = getApplication<Application>()

        val serviceIntent = Intent(context, DownloadService::class.java)
        context.startService(serviceIntent)
        context.bindService(serviceIntent, mConnection, 0)
    }

    private fun stopService() {
        val context = getApplication<Application>()
        context.unbindService(mConnection)
    }

    override fun parseUrl(url: String) : ParseFuture {

        val future = ParseFuture(url)

        parsingExecutors.execute(object : Runnable {

            override fun run() {

                if (!NetworkUtils.isNetworkAvailable(getApplication())) {
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

    fun parseUrlWithoutPagination(url: String) : ParseFuture {

        val future = ParseFuture(url)

        parsingExecutors.execute(object : Runnable {

            override fun run() {

                if (!NetworkUtils.isNetworkAvailable(getApplication())) {
                    future.failed("no network")
                    return
                }

                val isUrl : Boolean = NetworkUtils.isValidURL(url)

                if (!isUrl) {
                    future.failed("is not a valid website")
                    return
                }

                val parsingTask: ParsingTaskBase? = ParsingIdentifier.findHost(url)

                if (parsingTask == null) {
                    future.failed("could not find host")
                    return
                }

                val parsing : Boolean = parsingTask.parseMediaFile(url)

                if (!parsing) {
                    future.failed("could not find filetype")
                    return
                }

                future.success(ParsingData(parsingTask, null))
            }
        })

        return future
    }

    override fun parsePagination(url: String, paginationTask: PaginationParserTaskBase): PaginationParseFuture {

        val future = PaginationParseFuture(url, paginationTask)

        parsingExecutors.execute(object : Runnable {

            override fun run() {

                if (!NetworkUtils.isNetworkAvailable(getApplication())) {
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
                    val paginationFuture : ParseFuture = parseUrlWithoutPagination(paginationUrl)
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

                if (!NetworkUtils.isNetworkAvailable(getApplication())) {
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
                    val paginationFuture : ParseFuture = parseUrlWithoutPagination(paginationUrl)
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

            if (!NetworkUtils.isNetworkAvailable(getApplication())) {
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

        mDatabaseModel.insertDownloadableItem(item)
    }

    override fun retrieveItemsFromDB() {
        mDatabaseModel.retrieveAllDownloadableItems()
    }

    override fun emptyDB() {
        mDatabaseModel.deleteAllDownloadableItem()
    }

    override fun archive(downloadableItem: DownloadableItem) {
        downloadableItem.isArchived = true
        mDatabaseModel.updateDownloadableEntry(downloadableItem)
    }

    private val actionListener: DownloadableItemAction.Listener = object : DownloadableItemAction.Listener {

        override fun onPlay(action: DownloadableItemAction) {
            // no-ops
        }

        override fun onRefresh(action: DownloadableItemAction) {
            action.cancel()
            MediaUtils.deleteMediaFileIfExist(action.item)
            downloadService?.startDownload(action)
        }
    }
}
