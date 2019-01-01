package org.hugoandrade.rtpplaydownloader.network

enum class FileType(var mDownloaderTask: DownloaderTaskBase) {
    RTPPlay(RTPPlayDownloaderTask());

}