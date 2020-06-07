package org.hugoandrade.rtpplaydownloader.network

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.hugoandrade.rtpplaydownloader.DevConstants
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderIdentifier
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTask
import org.hugoandrade.rtpplaydownloader.network.download.RTPPlayTSDownloaderTask
import org.hugoandrade.rtpplaydownloader.network.download.TVIPlayerTSDownloaderTask
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingData
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationIdentifier
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationParserTask
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.ParsingIdentifier
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.ParsingMultiPartTaskBase
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.ParsingTask
import org.hugoandrade.rtpplaydownloader.network.persistence.DownloadableItemRepository
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.hugoandrade.rtpplaydownloader.utils.ListenableFuture
import org.hugoandrade.rtpplaydownloader.utils.NetworkUtils
import java.lang.RuntimeException
import java.lang.ref.WeakReference
import java.util.concurrent.*
import kotlin.collections.ArrayList

class DownloadManager(application: Application) : AndroidViewModel(application), DownloadManagerAPI {
    /**
     * Debugging tag used by the Android logger.
     */
    @Suppress("PrivatePropertyName", "unused")
    private val TAG = javaClass.simpleName

    private val downloadableItems: ArrayList<DownloadableItemAction> = ArrayList()
    private val downloadableItemsLiveData: MutableLiveData<ArrayList<DownloadableItemAction>> = MutableLiveData()

    private lateinit var mViewOps: WeakReference<DownloadManagerViewOps>

    private val parsingExecutors = Executors.newFixedThreadPool(DevConstants.nParsingThreads)

    private var mDatabaseModel: DownloadableItemRepository

    private var downloadService: DownloadService.DownloadServiceBinder? = null

    private val mConnection: ServiceConnection = ServiceConnectionImpl()

    init {
        startService()

        mDatabaseModel = DownloadableItemRepository(getApplication())

        retrieveItemsFromDB()
    }

    override fun getItems(): LiveData<ArrayList<DownloadableItemAction>> {
        return downloadableItemsLiveData
    }

    override fun attachCallback(viewOps: DownloadManagerViewOps) {
        mViewOps = WeakReference(viewOps)
    }

    override fun onCleared() {
        super.onCleared()

        parsingExecutors.shutdownNow()

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

    override fun parseUrl(url: String) : ListenableFuture<ParsingData> {

        val future = ListenableFuture<ParsingData>()

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

                val parsingTask: ParsingTask? = ParsingIdentifier.findHost(url)
                val paginationTask : PaginationParserTask? = PaginationIdentifier.findHost(url)

                if (parsingTask == null && paginationTask != null) {

                    try {
                        val f : ArrayList<ParsingTask>? = parsePagination(url, paginationTask).get()
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

                future.success(if (parsingTask is ParsingMultiPartTaskBase)
                    ParsingData(parsingTask.tasks, paginationTask) else
                    ParsingData(parsingTask, paginationTask))
            }
        })

        return future
    }

    override fun parsePagination(url: String, paginationTask: PaginationParserTask): ListenableFuture<ArrayList<ParsingTask>> {

        val future = ListenableFuture<ArrayList<ParsingTask>>()

        val task : Callable<ArrayList<ParsingTask>> = object: Callable<ArrayList<ParsingTask>> {


            override fun call(): ArrayList<ParsingTask> {

                if (!NetworkUtils.isNetworkAvailable(getApplication())) {
                    future.failed("no network")
                    throw RuntimeException("no network")
                }

                val paginationUrls = paginationTask.parsePagination(url)

                if (paginationUrls.size == 0) {
                    future.success(ArrayList())
                    return ArrayList()
                }


                val futures: List<Future<ParsingData>> = parsingExecutors.invokeAll(paginationUrls.map { paginationUrl -> parseUrlWithoutPagination(paginationUrl)})

                val paginationDownloadTasks = ArrayList<ParsingTask>()

                futures.forEach(action = { future ->

                    try{
                        paginationDownloadTasks.addAll(future.get().tasks)
                    }
                    catch (e : java.lang.Exception) {
                        Log.e(TAG, "error parsing pagination task ")
                        e.printStackTrace()
                    }
                })
                if (paginationDownloadTasks.size == 0) {
                    future.failed("no pagination urls found")
                    throw RuntimeException("no pagination urls found")
                }
                else {
                    future.success(paginationDownloadTasks)
                    return paginationDownloadTasks
                }
            }
        }
        parsingExecutors.submit(task)

        return future
    }

    override fun parseMore(url : String, paginationTask: PaginationParserTask): ListenableFuture<ArrayList<ParsingTask>> {

        val future = ListenableFuture<ArrayList<ParsingTask>>()

        val task : Callable<ArrayList<ParsingTask>> = object: Callable<ArrayList<ParsingTask>> {


            override fun call(): ArrayList<ParsingTask> {

                if (!NetworkUtils.isNetworkAvailable(getApplication())) {
                    future.failed("no network")
                    throw RuntimeException("no network")
                }

                val paginationUrls = paginationTask.parseMore()

                if (paginationUrls.size == 0) {
                    future.success(ArrayList())
                    return ArrayList()
                }


                val futures: List<Future<ParsingData>> = parsingExecutors.invokeAll(paginationUrls.map { paginationUrl -> parseUrlWithoutPagination(paginationUrl)})

                val paginationDownloadTasks = ArrayList<ParsingTask>()

                futures.forEach(action = { future ->

                    try{
                        paginationDownloadTasks.addAll(future.get().tasks)
                    }
                    catch (e : java.lang.Exception) {
                        Log.e(TAG, "error parsing pagination task ")
                        e.printStackTrace()
                    }
                })
                if (paginationDownloadTasks.size == 0) {
                    future.failed("no pagination urls found")
                    throw RuntimeException("no pagination urls found")
                }
                else {
                    future.success(paginationDownloadTasks)
                    return paginationDownloadTasks
                }
            }
        }
        
        parsingExecutors.submit(task)

        return future
    }

    fun parseUrlWithoutPagination(url: String) : Callable<ParsingData> {

        return Callable<ParsingData> {
            if (!NetworkUtils.isNetworkAvailable(getApplication())) {
                throw RuntimeException("no network")
            }

            val isUrl : Boolean = NetworkUtils.isValidURL(url)

            if (!isUrl) {
                throw RuntimeException("is not a valid website")
            }

            val parsingTask: ParsingTask? = ParsingIdentifier.findHost(url)

            if (parsingTask == null) {
                throw RuntimeException("could not find host")
            }

            val parsing : Boolean = parsingTask.parseMediaFile(url)

            if (!parsing) {
                throw RuntimeException("could not find filetype")
            }

            ParsingData(parsingTask, null)
        }
    }

    override fun download(task: ParsingTask)  {
        val url = task.url?: return
        val mediaUrl = task.mediaUrl?: return
        val filename = task.filename?: return
        val thumbnailUrl = task.thumbnailUrl

        val item = DownloadableItem(url = url, mediaUrl = mediaUrl, thumbnailUrl = thumbnailUrl, filename = filename)
        item.downloadTask = ParsingIdentifier.findType(task)?.name

        val future = mDatabaseModel.insertDownloadableItem(item)
        future.addCallback(object : ListenableFuture.Callback<DownloadableItem> {
            override fun onFailed(errorMessage: String) {
                Log.e(TAG, errorMessage)
            }

            override fun onSuccess(result: DownloadableItem) {

                val downloadableItem = result
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

                val downloadableItems = ArrayList<DownloadableItemAction>()
                downloadableItems.add(action)
                downloadableItems.sortedWith(compareBy { it.item.id } )

                mViewOps.get()?.displayDownloadableItem(action)
            }

        })
    }

    override fun retrieveItemsFromDB() {

        val future = mDatabaseModel.retrieveAllDownloadableItems()
        future.addCallback(object : ListenableFuture.Callback<List<DownloadableItem>> {
            override fun onFailed(errorMessage: String) {
                Log.e(TAG, errorMessage)
            }

            override fun onSuccess(result: List<DownloadableItem>) {

                val downloadableItems = result
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

                downloadableItemsLiveData.postValue(this@DownloadManager.downloadableItems)
            }

        })
    }

    override fun emptyDB() {
        val future = mDatabaseModel.deleteAllDownloadableItem()
        future.addCallback(object : ListenableFuture.Callback<Boolean> {
            override fun onFailed(errorMessage: String) {
                Log.e(TAG, errorMessage)
            }

            override fun onSuccess(result: Boolean) {
                downloadableItems.clear()
                downloadableItemsLiveData.postValue(downloadableItems)
            }
        })
    }

    override fun archive(downloadableItem: DownloadableItem) {
        downloadableItem.isArchived = true
        mDatabaseModel.updateDownloadableEntry(downloadableItem)
    }

    private fun isPaginationUrlTheSameOfTheOriginalUrl(url: String,
                                                       parsingTask: ParsingTask,
                                                       paginationTask: PaginationParserTask?): Boolean {

        if (paginationTask == null) {
            return false
        }

        val paginationUrls : ArrayList<String> = paginationTask.parsePagination(url)

        if (paginationUrls.size == 0) {
            return false
        }

        val tasks : ArrayList<ParsingTask> = if (parsingTask is ParsingMultiPartTaskBase) {
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

            val paginationParsingTask: ParsingTask = ParsingIdentifier.findHost(p) ?: return false

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

    inner class ServiceConnectionImpl : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            downloadService = (service as DownloadService.DownloadServiceBinder)

            val downloadService = this@DownloadManager.downloadService
            val activeItemActions : ArrayList<DownloadableItemAction> =  downloadService?.getItemActions()?:return

            // populate with items which are being downloaded
            for (itemAction in activeItemActions) {

                synchronized(downloadableItems) {
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

            downloadableItemsLiveData.postValue(downloadableItems)
        }

        override fun onServiceDisconnected(className: ComponentName) {
            downloadService = null
        }
    }
}
