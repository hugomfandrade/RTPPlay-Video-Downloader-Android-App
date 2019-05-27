package org.hugoandrade.rtpplaydownloader.network.download

abstract class DownloaderMultiPartTaskBase : DownloaderTaskBase() {

    var tasks : ArrayList<DownloaderTaskBase> = ArrayList()
}