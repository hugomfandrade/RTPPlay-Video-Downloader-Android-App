package org.hugoandrade.rtpplaydownloader.network.parsing

import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTaskBase
import org.hugoandrade.rtpplaydownloader.network.download.RTPPlayDownloaderTask
import org.hugoandrade.rtpplaydownloader.network.download.SAPODownloaderTask
import org.hugoandrade.rtpplaydownloader.network.download.SICDownloaderTask

enum class FileType(var downloaderTask: DownloaderTaskBase) {
    RTPPlay(RTPPlayDownloaderTask()),
    SIC(SICDownloaderTask()),
    SAPO(SAPODownloaderTask())
}