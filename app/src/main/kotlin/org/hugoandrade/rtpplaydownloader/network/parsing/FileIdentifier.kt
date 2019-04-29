package org.hugoandrade.rtpplaydownloader.network.parsing

import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTaskBase
import org.hugoandrade.rtpplaydownloader.network.download.RTPPlayDownloaderTask
import org.hugoandrade.rtpplaydownloader.network.download.SAPODownloaderTask
import org.hugoandrade.rtpplaydownloader.network.download.SICDownloaderTask

class FileIdentifier() {

    init {
        throw AssertionError()
    }

    companion object {

        fun findHost(urlString: String): DownloaderTaskBase? {
            for (fileType: FileType in FileType.values()) {
                if (fileType.downloaderTask.isValid(urlString)) {
                    when (fileType) {
                        FileType.RTPPlay -> return RTPPlayDownloaderTask()
                        FileType.SIC -> return SICDownloaderTask()
                        FileType.SAPO -> return SAPODownloaderTask()
                    }
                }
            }
            return null
        }
    }
}