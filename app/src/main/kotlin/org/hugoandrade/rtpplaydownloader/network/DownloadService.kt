package org.hugoandrade.rtpplaydownloader.network

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import org.hugoandrade.rtpplaydownloader.DevConstants
import org.hugoandrade.rtpplaydownloader.MainActivity
import org.hugoandrade.rtpplaydownloader.R
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.LinkedHashMap
import kotlin.collections.set
import kotlin.math.roundToInt


class DownloadService : Service() {

    companion object  {
        val CHANNEL_ID = "DownloadServiceChannel"
        val CHANNEL_NAME = "Download Service Channel"

        val DELETE_KEY = "DeleteKey"
        val DELETE_VALUE = 50

        val NOTIFICATION_ID = 1
    }

    private val downloadExecutors = Executors.newFixedThreadPool(DevConstants.nDownloadThreads)

    private val mBinder = DownloadServiceBinder()

    override fun onCreate() {
        super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()

        downloadExecutors.shutdown()
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        if (intent.hasExtra(DELETE_KEY)) {
            downloadMap.values.forEach{
                a -> a.downloaderTask.cancel()
            }
        }

        return START_NOT_STICKY
    }

    @Synchronized
    private fun setForeground() {
        createNotificationChannel()

        startForeground(NOTIFICATION_ID, createNotification("", 0))

        updateTimer.cancel()
        updateTimer = Timer()
        updateTimer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                updateNotification()
            }
        }, 1000, 1000)
    }

    private var updateTimer : Timer = Timer()

    private fun createNotification(text: String, progress: Int): Notification {

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
                .setContentTitle(getString(R.string.app_name))
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setDeleteIntent(deletePendingIntent)
                .addAction(R.mipmap.ic_launcher, getString(R.string.cancel), pStopSelf)
                .setProgress(100, progress, false)
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
            var progress = 0
            for (action in actions) {
                if (longestRemainingTime < action.item.remainingTime) {
                    longestRemainingTime = action.item.remainingTime
                    progress = (action.item.progress * 100f).roundToInt()
                }
            }

            val text = actions[0].item.filename +
                    if (actions.size == 1) "" else " and " + (actions.size - 1) + "other videos"

            val notification : Notification = createNotification(text, progress)

            val manager = ContextCompat.getSystemService(applicationContext, NotificationManager::class.java)
            manager?.notify(NOTIFICATION_ID, notification)
        }
    }

    private val downloadMap : LinkedHashMap<Int, DownloadableItemAction> = LinkedHashMap()

    private fun start(downloadableItemAction: DownloadableItemAction) {
        if (downloadMap.containsKey(downloadableItemAction.item.id)) return

        if (downloadMap.isEmpty()) {
            setForeground()
        }
        downloadMap[downloadableItemAction.item.id] = downloadableItemAction

        updateNotification()

        downloadableItemAction.item.addDownloadStateChangeListener(object : DownloadableItemState.ChangeListener {

            override fun onDownloadStateChange(downloadableItem: DownloadableItem) {
                if (downloadableItem.state == DownloadableItemState.End ||
                    downloadableItem.state == DownloadableItemState.Failed) {
                    downloadMap.remove(downloadableItem.id)

                    updateNotification()
                }
            }
        })

        downloadExecutors.execute {
            downloadableItemAction.downloaderTask.downloadMediaFile(downloadableItemAction)
            downloadMap.remove(downloadableItemAction.item.id)

            updateNotification()
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