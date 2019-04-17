package org.hugoandrade.rtpplaydownloader.network.parsing

import org.hugoandrade.rtpplaydownloader.network.DownloaderTaskBase
import org.hugoandrade.rtpplaydownloader.network.RTPPlayDownloaderTask

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
                    }
                }
            }
            return null
        }
    }
}