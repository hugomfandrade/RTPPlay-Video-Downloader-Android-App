package org.hugoandrade.rtpplaydownloader.network.download

import android.net.Uri
import org.hugoandrade.rtpplaydownloader.network.DownloadableItem
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.ParsingIdentifier.FileType
import java.lang.RuntimeException

class DownloaderIdentifier() {

    init {
        throw AssertionError()
    }

    companion object {

        val TAG = "DownloaderIdentifier"

        fun findHost(downloadTask: String?, mediaUrl: String): DownloadType? {

            for (fileType: FileType in FileType.values()) {
                if (fileType.name == downloadTask) {

                    when (fileType) {
                        FileType.TVIPlayer -> return DownloadType.TVITSFiles
                        FileType.RTPPlay -> {
                            return if (mediaUrl.contains(".m3u8")) DownloadType.RTPTSFiles else DownloadType.FullFile
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

            val url = downloadableItem.url
            val mediaUrl = downloadableItem.mediaUrl ?: throw IllegalAccessException("mediaUrl not found")
            val filename = downloadableItem.filename ?: throw IllegalAccessException("filename not found")
            val downloadTask = downloadableItem.downloadTask

            return when(findHost(downloadTask, mediaUrl)) {
                DownloadType.FullFile -> DownloaderTask(mediaUrl, dir, filename, listener)
                DownloadType.TVITSFiles -> TVIPlayerTSDownloaderTask(url, mediaUrl, dir, filename, listener)
                DownloadType.RTPTSFiles -> {
                    if (filename.endsWith(".mp3")) DownloaderTask(mediaUrl, dir, filename, listener)
                    else  RTPPlayTSDownloaderTask(url, mediaUrl, dir, filename, listener)
                }
                DownloadType.SICTSFiles -> {
                    if (filename.endsWith("net_wide")) DownloaderTask(mediaUrl, dir, filename, listener)
                    else  SICTSDownloaderTask(url, mediaUrl, dir, filename, listener)
                }
                null -> throw IllegalAccessException("downloaderTask not found")
            }
        }
    }

    enum class DownloadType {
        FullFile,
        TVITSFiles,
        RTPTSFiles,
        SICTSFiles
    }
}