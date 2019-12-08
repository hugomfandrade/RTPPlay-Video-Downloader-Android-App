package org.hugoandrade.rtpplaydownloader.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import org.hugoandrade.rtpplaydownloader.DevConstants
import org.hugoandrade.rtpplaydownloader.MainActivity
import org.hugoandrade.rtpplaydownloader.R
import java.util.ArrayList
import java.util.concurrent.Executors

class DownloadService : Service() {

    companion object  {
        val CHANNEL_ID = "DownloadServiceChannel"
    }

    private val downloadExecutors = Executors.newFixedThreadPool(DevConstants.nDownloadThreads)

    private val mBinder = DownloadServiceBinder()

    override fun onCreate() {
        super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    private fun updateNotification() {
        if (getItemActions().isEmpty()) {
            stopForeground(true)
        }
        else {

            createNotificationChannel()
            val notificationIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(this,
                    0, notificationIntent, 0)
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(getString(R.string.app_name))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(pendingIntent)
                    .build()
            startForeground(1, notification)
        }
    }

    private val downloadMap : HashMap<Int, DownloadableItemAction> = HashMap()

    private fun start(downloadableItemAction: DownloadableItemAction) {
        System.err.println("start = " + downloadableItemAction.item.url + " -> already in line to download ? "+ downloadMap.containsKey(downloadableItemAction.item.id))
        if (downloadMap.containsKey(downloadableItemAction.item.id)) return
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

    /* class IncomingHandler extends Handler {

        @Override
        public void handleMessage( Message msg ){
            switch(msg.what) {
                case START:
                    startService( new Intent( this, DownloadService.class ) );
                    startForeground( MY_NOTIFICATION, makeNotification() );
                    break;

                case STOP:
                    stopForeground( true );
                    stopSelf();
                    break;

                default:
                    super.handleMessage( msg );
            }
        }
    }*/

}