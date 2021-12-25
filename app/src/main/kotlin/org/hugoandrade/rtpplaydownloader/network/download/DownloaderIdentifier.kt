package org.hugoandrade.rtpplaydownloader.network.download

import android.net.Uri
import org.hugoandrade.rtpplaydownloader.network.DownloadableItem
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.ParsingIdentifier.FileType

class DownloaderIdentifier {

    init {
        throw AssertionError()
    }

    companion object {

        val TAG = "DownloaderIdentifier"

        fun findHost(downloadTask: String?, mediaUrl: String): DownloadType {

            for (fileType: FileType in FileType.values()) {
                if (fileType.name == downloadTask) {

                    when (fileType) {
                        FileType.TVIPlayer -> return DownloadType.TVITSFiles
                        FileType.RTPPlay -> {
                            return when {
                                mediaUrl.contains(".m3u8") -> DownloadType.RTPTSFiles
                                mediaUrl.contains(".mp4") -> DownloadType.RTPTSFiles
                                else -> DownloadType.FullFile
                            }
                        }
                        FileType.SIC -> {
                            return if (mediaUrl.contains(".m3u8")) DownloadType.SICTSFiles else DownloadType.FullFile
                        }
                        else -> DownloadType.FullFile
                    }
                }
            }
            return DownloadType.FullFile
        }

        @Throws(IllegalAccessException::class)
        fun findTask(dirPath: Uri, downloadableItem: DownloadableItem): DownloaderTask {
            return findTask(dirPath.toString(), downloadableItem, downloadableItem)
        }

        @Throws(IllegalAccessException::class)
        fun findTask(dir: String, downloadableItem: DownloadableItem, listener : DownloaderTask.Listener): DownloaderTask {

            val mediaUrl = downloadableItem.mediaUrl ?: throw IllegalAccessException("mediaUrl not found")
            val filename = downloadableItem.filename ?: throw IllegalAccessException("filename not found")

            if (mediaUrl.contains(".m3u8")) {
                return TSDownloaderTask(mediaUrl, dir, filename, listener)
            }
            else {
                return RawDownloaderTask(mediaUrl, dir, filename, listener)
            }
            /*
            val downloadTask = downloadableItem.downloadTask

            return when(findHost(downloadTask, mediaUrl)) {
                DownloadType.FullFile -> RawDownloaderTask(mediaUrl, dir, filename, listener)
                DownloadType.TVITSFiles -> TSDownloaderTask(mediaUrl, dir, filename, listener)
                DownloadType.RTPTSFiles -> {
                    if (filename.endsWith(".mp3")) RawDownloaderTask(mediaUrl, dir, filename, listener)
                    else  TSDownloaderTask(mediaUrl, dir, filename, listener)
                }
                DownloadType.SICTSFiles -> {
                    if (filename.endsWith("net_wide")) RawDownloaderTask(mediaUrl, dir, filename, listener)
                    else  TSDownloaderTask(mediaUrl, dir, filename, listener)
                }
            }
            */
        }
    }

    enum class DownloadType {
        FullFile,

        TVITSFiles,
        RTPTSFiles,
        SICTSFiles
    }
}