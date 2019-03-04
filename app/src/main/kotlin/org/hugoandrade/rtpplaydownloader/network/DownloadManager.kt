package org.hugoandrade.rtpplaydownloader.network

import java.lang.ref.WeakReference

class DownloadManager  {

    /**
     * Debugging tag used by the Android logger.
     */
    protected var TAG = javaClass.simpleName

    private lateinit var mViewOps: WeakReference<DownloadManagerViewOps>

    fun onCreate(viewOps: DownloadManagerViewOps) {
        mViewOps = WeakReference(viewOps);
    }

    fun onDestroy() {
    }

    fun start(urlText: String) : DownloadableItem {

        return DownloadableItem(urlText, mViewOps.get()).start()
    }
}
