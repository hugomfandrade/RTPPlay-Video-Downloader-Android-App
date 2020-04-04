package org.hugoandrade.rtpplaydownloader.network.download

import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.ParsingIdentifier.FileType

class DownloaderIdentifier() {

    init {
        throw AssertionError()
    }

    companion object {

        fun findHost(downloadTask: String?, mediaUrl: String): DownloadType? {
            for (fileType: FileType in FileType.values()) {
                if (fileType.name == downloadTask) {
                    when (fileType) {
                        FileType.TVIPlayer -> return DownloadType.TVITSFiles
                        FileType.RTPPlay -> {
                            return if (mediaUrl.contains(".m3u8")) DownloadType.RTPTSFiles else DownloadType.FullFile
                        }
                        else -> DownloadType.FullFile
                    }
                }
            }
            return DownloadType.FullFile
        }
    }

    enum class DownloadType {
        FullFile,
        TVITSFiles,
        RTPTSFiles
    }
}