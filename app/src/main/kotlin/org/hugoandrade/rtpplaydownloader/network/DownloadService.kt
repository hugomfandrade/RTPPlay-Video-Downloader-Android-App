package org.hugoandrade.rtpplaydownloader.network

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import org.hugoandrade.rtpplaydownloader.DevConstants
import org.hugoandrade.rtpplaydownloader.app.main.MainActivity
import org.hugoandrade.rtpplaydownloader.R
import org.hugoandrade.rtpplaydownloader.network.persistence.DatabaseModel
import org.hugoandrade.rtpplaydownloader.network.persistence.PersistencePresenterOps
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.LinkedHashMap
import kotlin.collections.set
import kotlin.math.max

class DownloadService : Service() {

    companion object  {
        const val CHANNEL_ID = "DownloadServiceChannel"
        const val CHANNEL_NAME = "Download Service Channel"

        const val DELETE_KEY = "DeleteKey"
        const val DELETE_VALUE = 50

        const val NOTIFICATION_ID = 1
    }

    private val downloadExecutors = Executors.newFixedThreadPool(DevConstants.nDownloadThreads)

    private val mBinder = DownloadServiceBinder()

    private lateinit var mDatabaseModel: DatabaseModel

    override fun onCreate() {
        super.onCreate()

        val databaseModel = object : DatabaseModel(){}
        databaseModel.onCreate(mPersistencePresenterOps)

        mDatabaseModel = databaseModel
    }

    override fun onDestroy() {
        super.onDestroy()

        mDatabaseModel.onDestroy()
        downloadExecutors.shutdown()
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    val mPersistencePresenterOps = object : PersistencePresenterOps {

        override fun getActivityContext(): Context? {
            return this@DownloadService.applicationContext
        }

        override fun getApplicationContext(): Context? {
            return this@DownloadService
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            )
            serviceChannel.enableLights(false)
            serviceChannel.enableVibration(false)
            serviceChannel.vibrationPattern = LongArray(0)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        if (intent.hasExtra(DELETE_KEY)) {
            downloadMap.values.forEach{
                a -> a.downloadTask.cancel()
            }
            downloadMap.clear()
            updateTimer.cancel()
            stopForeground(true)
        }

        return START_NOT_STICKY
    }

    @Synchronized
    private fun setForeground() {
        createNotificationChannel()

        startForeground(NOTIFICATION_ID, createNotification("", "",0, 1))

        updateTimer.cancel()
        updateTimer = Timer()
        updateTimer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                updateNotification()
            }
        }, 1000, 1000)
    }

    private var updateTimer : Timer = Timer()

    private fun createNotification(title: String, text: String, progress: Int, max: Int): Notification {

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0)

        val deleteIntent = Intent(this, DownloadService::class.java)
        deleteIntent.putExtra(DELETE_KEY, DELETE_VALUE)
        val deletePendingIntent = PendingIntent.getService(this,
                DELETE_VALUE,
                deleteIntent,
                PendingIntent.FLAG_CANCEL_CURRENT)

        val stopSelf = Intent(this, DownloadService::class.java)
        stopSelf.action = DELETE_KEY
        stopSelf.putExtra(DELETE_KEY, DELETE_VALUE)
        val pStopSelf = PendingIntent.getService(this, 0, stopSelf, PendingIntent.FLAG_CANCEL_CURRENT)

        return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setDeleteIntent(deletePendingIntent)
                .addAction(R.mipmap.ic_launcher, getString(R.string.cancel), pStopSelf)
                .setProgress(max, progress, false)
                .build()
    }

    private fun updateNotification() {
        val actions = getItemActions()
        if (actions.isEmpty()) {
            stopForeground(true)
            updateTimer.cancel()
        }
        else {

            var longestRemainingTime = 0L
            var progress = 0L
            var maxProgress = 0L
            for (action in actions) {
                if (longestRemainingTime < action.item.remainingTime) {
                    longestRemainingTime = action.item.remainingTime
                    val filesize = action.item.filesize

                    if (filesize != null) {
                        progress += action.item.progressSize
                        maxProgress += filesize
                    }
                }
            }

            val title = actions[0].item.filename +
                    if (actions.size == 1)
                        "" else
                        (" " + getString(R.string.and) + " ") + (actions.size - 1) + " " +
                                (if (actions.size == 2)
                                    getString(R.string.other_video) else
                                    getString(R.string.other_videos))
            val text = MediaUtils.humanReadableTime(longestRemainingTime) + " " + getString(R.string.remaining_time)

            val notification : Notification = createNotification(
                    title,
                    if (text.startsWith("0s")) "" else text,
                    ((progress * 100L) / max(maxProgress, 1L)).toInt(),
                    100)

            val manager = ContextCompat.getSystemService(applicationContext, NotificationManager::class.java)
            manager?.notify(NOTIFICATION_ID, notification)
        }
    }

    private val downloadMap : LinkedHashMap<Int, DownloadableItemAction> = LinkedHashMap()

    private fun start(downloadableItemAction: DownloadableItemAction) {
        val itemID: Int = downloadableItemAction.item.id

        if (downloadMap.containsKey(downloadableItemAction.item.id)) return

        if (downloadMap.isEmpty()) {
            setForeground()
        }

        downloadMap[itemID] = downloadableItemAction

        updateNotification()

        downloadableItemAction.item.addDownloadStateChangeListener(object : DownloadableItem.State.ChangeListener {

            override fun onDownloadStateChange(downloadableItem: DownloadableItem) {

                if (downloadableItem.state == DownloadableItem.State.End ||
                        downloadableItem.state == DownloadableItem.State.Failed) {

                    downloadMap.remove(downloadableItem.id)

                    updateNotification()

                    if (downloadableItem.state == DownloadableItem.State.End) {
                        val filePath = downloadableItem.filepath
                        if (filePath != null) {
                            MediaScannerConnection.scanFile(this@DownloadService, arrayOf(filePath.toString()), null, null)
                        }
                    }

                    // downloadableItemAction.item.removeDownloadStateChangeListener(this)
                }
                else if (downloadableItem.state == DownloadableItem.State.Start) {

                    if (downloadMap.isEmpty()) {
                        setForeground()
                    }

                    downloadMap[itemID] = downloadableItemAction

                    updateNotification()
                }

                mDatabaseModel.updateDownloadableEntry(downloadableItem)
            }
        })

        downloadExecutors.execute {
            downloadableItemAction.downloadTask.downloadMediaFile()
        }
    }

    // NOT YET USED
    private val changeListener = object : DownloadableItem.State.ChangeListener {

        override fun onDownloadStateChange(downloadableItem: DownloadableItem) {
            val itemID: Int = downloadableItem.id

            if (downloadableItem.state == DownloadableItem.State.End ||
                    downloadableItem.state == DownloadableItem.State.Failed) {

                downloadMap.remove(downloadableItem.id)

                updateNotification()

                if (downloadableItem.state == DownloadableItem.State.End) {
                    val filePath = downloadableItem.filepath
                    if (filePath != null) {
                        MediaScannerConnection.scanFile(this@DownloadService, arrayOf(filePath.toString()), null, null)
                    }
                }

                // downloadableItemAction.item.removeDownloadStateChangeListener(this)
            }
            else if (downloadableItem.state == DownloadableItem.State.Start) {

                if (downloadMap.isEmpty()) {
                    setForeground()
                }

                // downloadMap[itemID] = downloadableItemAction

                updateNotification()
            }

            mDatabaseModel.updateDownloadableEntry(downloadableItem)
        }
    }

    private fun getItemActions(): ArrayList<DownloadableItemAction> {
        return ArrayList(downloadMap.values)
    }

    inner class DownloadServiceBinder : Binder() {

        fun startDownload(downloadableItemAction: DownloadableItemAction) {
            this@DownloadService.start(downloadableItemAction)
        }

        fun getItemActions(): ArrayList<DownloadableItemAction> {
            return this@DownloadService.getItemActions()
        }
    }
}