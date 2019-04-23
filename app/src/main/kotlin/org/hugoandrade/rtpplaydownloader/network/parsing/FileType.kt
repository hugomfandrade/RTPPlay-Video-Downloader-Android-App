package org.hugoandrade.rtpplaydownloader.network.parsing

import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTaskBase
import org.hugoandrade.rtpplaydownloader.network.download.RTPPlayDownloaderTask

enum class FileType(var downloaderTask: DownloaderTaskBase) {
    RTPPlay(RTPPlayDownloaderTask())
}