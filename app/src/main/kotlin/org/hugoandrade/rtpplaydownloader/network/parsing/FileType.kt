package org.hugoandrade.rtpplaydownloader.network.parsing

import org.hugoandrade.rtpplaydownloader.network.DownloaderTaskBase
import org.hugoandrade.rtpplaydownloader.network.RTPPlayDownloaderTask

enum class FileType(var downloaderTask: DownloaderTaskBase) {
    RTPPlay(RTPPlayDownloaderTask())
}