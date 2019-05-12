package org.hugoandrade.rtpplaydownloader.network

import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTaskBase
import org.hugoandrade.rtpplaydownloader.network.parsing.FileIdentifier
import org.hugoandrade.rtpplaydownloader.network.parsing.ParseFuture
import org.hugoandrade.rtpplaydownloader.network.persistance.DatabaseModel
import org.hugoandrade.rtpplaydownloader.network.persistance.DownloadableEntry
import org.hugoandrade.rtpplaydownloader.network.persistance.DownloadableEntryParser
import org.hugoandrade.rtpplaydownloader.utils.NetworkUtils
import java.lang.ref.WeakReference

class DownloadManager  {

    /**
     * Debugging tag used by the Android logger.
     */
    @Suppress("PrivatePropertyName", "unused")
    private val TAG = javaClass.simpleName

    private lateinit var mViewOps: WeakReference<DownloadManagerViewOps>

    private lateinit var mDatabaseModel: DatabaseModel

    fun onCreate(viewOps: DownloadManagerViewOps) {
        mViewOps = WeakReference(viewOps)

        mDatabaseModel = object : DatabaseModel() {
            override fun onGetAllDownloadableEntries(downloadableEntries: List<DownloadableEntry>) {
                mViewOps.get()?.populateDownloadableItemsRecyclerView(DownloadableEntryParser.formatCollection(downloadableEntries))
            }

            override fun onDeleteAllDownloadableEntries(downloadableEntries: List<DownloadableEntry>) {
                mViewOps.get()?.populateDownloadableItemsRecyclerView(ArrayList<DownloadableItem>())
            }

            override fun onResetDatabase(wasSuccessfullyDeleted : Boolean){
                mViewOps.get()?.populateDownloadableItemsRecyclerView(ArrayList<DownloadableItem>())
            }
        }
        mDatabaseModel.onInitialize(viewOps.getActivityContext())
        mDatabaseModel.open()
    }

    fun onDestroy() {
        mDatabaseModel.close()
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
        val downloadableItem = DownloadableItem(task, mViewOps.get())
        downloadableItem.addDownloadStateChangeListener(object :DownloadableItemStateChangeListener {
            override fun onDownloadStateChange(downloadableItem: DownloadableItem) {
                // TODO
                if (downloadableItem.state == DownloadableItemState.Start) {
                    mDatabaseModel.insertDownloadableEntry(DownloadableEntryParser.parse(downloadableItem))
                }
                else {
                    mDatabaseModel.updateDownloadableEntry(DownloadableEntryParser.parse(downloadableItem))
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
}
