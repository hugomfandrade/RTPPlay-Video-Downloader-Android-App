package org.hugoandrade.rtpplaydownloader.network.parsing

import com.google.common.util.concurrent.AtomicDouble
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTask
import org.hugoandrade.rtpplaydownloader.network.download.RTPPlayTSDownloaderTask
import org.hugoandrade.rtpplaydownloader.network.download.SICTSDownloaderTask
import org.hugoandrade.rtpplaydownloader.network.download.TVIPlayerTSDownloaderTask
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.*
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.hugoandrade.rtpplaydownloader.network.utils.NetworkUtils
import org.junit.Test
import java.io.File
import kotlin.math.roundToInt

class ParsingUnitTest {

    private val testDir = File("test-download-folder")
    private val defaultListener: DownloaderTask.Listener = object : DownloaderTask.Listener {

        private val progressLogPercentageDelta = 1.0
        private val progressLogLastPercentage = AtomicDouble(Double.NaN)

        override fun downloadStarted(f: File) {
            System.err.println("downloadStarted " + f)
            progressLogLastPercentage.set(Double.NaN)
        }

        override fun onProgress(downloadedSize: Long, totalSize: Long) {

            val progress = downloadedSize.toDouble() / totalSize.toDouble() * 100
            val readableDownloadSize = MediaUtils.humanReadableByteCount(downloadedSize, true)
            val readableTotalSize = MediaUtils.humanReadableByteCount(totalSize, true)

            if (!progressLogPercentageDelta.isNaN()) {
                when {
                    progressLogLastPercentage.get().isNaN() -> {
                        progressLogLastPercentage.set(progress + progressLogPercentageDelta)
                    }
                    progressLogLastPercentage.get() < progress -> {
                        progressLogLastPercentage.set(progress + progressLogPercentageDelta)
                    }
                    else -> {
                        return
                    }
                }
            }

            // System.err.println("onProgress " + downloadedSize + " - " + totalSize)
            System.err.println("onProgress " + progress.roundToInt() + "%" + " (" + readableTotalSize + ")")
        }

        override fun downloadFinished(f: File) {
            System.err.println("downloadFinished " + f)
        }

        override fun downloadFailed(message: String?) {
            System.err.println("downloadFailed " + message)
        }
    }

    @Test
    @Deprecated(message = "no longer valid")
    fun sicPlayer() {

        val url = "https://sic.pt/Programas/governo-sombra/videos/2020-07-18-Governo-Sombra---17-de-julho"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val isUrl : Boolean = NetworkUtils.isValidURL(url)

        if (!isUrl) throw RuntimeException("is not a valid website")

        val parsingTask = SICParsingTask()
        parsingTask.parseMediaFile(url)

        val mediaUrl : String? = parsingTask.mediaUrl
        val mediaFilename : String = MediaUtils.getUniqueFilenameAndLock(testDir.absolutePath, parsingTask.filename ?: "")

        System.err.println("successfully parsed: ")
        System.err.println(mediaUrl)
        System.err.println(mediaFilename)

        val sicTSDownloaderTask = DownloaderTask(mediaUrl?:"", testDir.absolutePath, mediaFilename, defaultListener)
        sicTSDownloaderTask.downloadMediaFile()
    }

    @Test
    @Deprecated(message = "no longer valid")
    fun sicV2Player() {

        val url = "https://sic.pt/Programas/governo-sombra/videos/2020-08-29-Governo-Sombra---28-de-agosto"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val isUrl : Boolean = NetworkUtils.isValidURL(url)

        if (!isUrl) throw RuntimeException("is not a valid website")

        val parsingTask = SICParsingTaskV2()
        parsingTask.parseMediaFile(url)

        val mediaUrl : String? = parsingTask.mediaUrl
        val mediaFilename : String = MediaUtils.getUniqueFilenameAndLock(testDir.absolutePath, parsingTask.filename ?: "")

        System.err.println("successfully parsed: ")
        System.err.println(mediaUrl)
        System.err.println(mediaFilename)

        val sicTSDownloaderTask = SICTSDownloaderTask(url, mediaUrl?:"", testDir.absolutePath, mediaFilename, defaultListener)
        sicTSDownloaderTask.downloadMediaFile()
    }

    @Test
    fun sicV3Player() {

        val url = "https://sicnoticias.pt/programas/eixodomal/2020-09-11-Regresso-as-aulas-Presidenciais-e-a-morte-de-Vicente-Jorge-Silva.-O-Eixo-do-Mal-na-integra"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val isUrl : Boolean = NetworkUtils.isValidURL(url)

        if (!isUrl) throw RuntimeException("is not a valid website")

        val parsingTask = SICParsingTaskV3()
        parsingTask.parseMediaFile(url)

        val mediaUrl : String? = parsingTask.mediaUrl
        val mediaFilename : String = MediaUtils.getUniqueFilenameAndLock(testDir.absolutePath, parsingTask.filename ?: "")

        System.err.println("successfully parsed: ")
        System.err.println(mediaUrl)
        System.err.println(mediaFilename)

        val sicTSDownloaderTask = SICTSDownloaderTask(url, mediaUrl?:"", testDir.absolutePath, mediaFilename, defaultListener)
        sicTSDownloaderTask.downloadMediaFile()
    }

    @Test
    fun sicV3PlayerSICRadical() {

        val url = "https://sicradical.pt/programas/irritacoes/Videos/2020-09-11-Irritacoes---Programa-de-11-de-setembro"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val isUrl : Boolean = NetworkUtils.isValidURL(url)

        if (!isUrl) throw RuntimeException("is not a valid website")

        val parsingTask = SICParsingTaskV3()
        parsingTask.parseMediaFile(url)

        val mediaUrl : String? = parsingTask.mediaUrl
        val mediaFilename : String = MediaUtils.getUniqueFilenameAndLock(testDir.absolutePath, parsingTask.filename ?: "")

        System.err.println("successfully parsed: ")
        System.err.println(mediaUrl)
        System.err.println(mediaFilename)

        val sicTSDownloaderTask = SICTSDownloaderTask(url, mediaUrl?:"", testDir.absolutePath, mediaFilename, defaultListener)
        sicTSDownloaderTask.downloadMediaFile()
    }

    @Test
    fun tviPlayerPlayer() {

        val url = "https://tviplayer.iol.pt/programa/circulatura-do-quadrado/5c4b41730cf2a84eaefc024b"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val isUrl : Boolean = NetworkUtils.isValidURL(url)

        if (!isUrl) throw RuntimeException("is not a valid website")

        val parsingTask = TVIPlayerParsingTask()
        parsingTask.parseMediaFile(url)

        val mediaUrl : String? = parsingTask.mediaUrl
        val mediaFilename : String = MediaUtils.getUniqueFilenameAndLock(testDir.absolutePath, parsingTask.filename ?: "")

        System.err.println("successfully parsed: ")
        System.err.println(mediaUrl)
        System.err.println(mediaFilename)

        val tviPlayerTSDownloaderTask = TVIPlayerTSDownloaderTask(url, mediaUrl?:"", testDir.absolutePath, mediaFilename, defaultListener)
        tviPlayerTSDownloaderTask.downloadMediaFile()
    }

    @Test
    @Deprecated(message = "no longer valid")
    fun rtpPlayV1Player() {

        val url = "https://www.rtp.pt/play/p2064/gps"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val isUrl : Boolean = NetworkUtils.isValidURL(url)

        if (!isUrl) throw RuntimeException("is not a valid website")

        val parsingTask = RTPPlayParsingTask()
        parsingTask.parseMediaFile(url)

        val mediaUrl : String? = parsingTask.mediaUrl
        val mediaFilename : String = MediaUtils.getUniqueFilenameAndLock(testDir.absolutePath, parsingTask.filename ?: "")

        System.err.println("successfully parsed: ")
        System.err.println(mediaUrl)
        System.err.println(mediaFilename)

        val downloaderTask = DownloaderTask(mediaUrl?:"", testDir.absolutePath, mediaFilename, defaultListener)
        downloaderTask.downloadMediaFile()
    }

    @Test
    @Deprecated(message = "no longer valid")
    fun rtpPlayV2Player() {

        val url = "https://www.rtp.pt/play/p2064/gps"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val isUrl : Boolean = NetworkUtils.isValidURL(url)

        if (!isUrl) throw RuntimeException("is not a valid website")

        val parsingTask = RTPPlayParsingTaskV2()
        parsingTask.parseMediaFile(url)

        val mediaUrl : String? = parsingTask.mediaUrl
        val mediaFilename : String = MediaUtils.getUniqueFilenameAndLock(testDir.absolutePath, parsingTask.filename ?: "")

        System.err.println("successfully parsed: ")
        System.err.println(mediaUrl)
        System.err.println(mediaFilename)

        val downloaderTask = DownloaderTask(mediaUrl?:"", testDir.absolutePath, mediaFilename, defaultListener)
        downloaderTask.downloadMediaFile()
    }

    @Test
    fun rtpPlayV3Player() {

        val url = "https://www.rtp.pt/play/p2064/gps"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val isUrl : Boolean = NetworkUtils.isValidURL(url)

        if (!isUrl) throw RuntimeException("is not a valid website")

        val parsingTask = RTPPlayParsingTaskV3()
        parsingTask.parseMediaFile(url)

        val mediaUrl : String? = parsingTask.mediaUrl
        val mediaFilename : String = MediaUtils.getUniqueFilenameAndLock(testDir.absolutePath, parsingTask.filename ?: "")

        System.err.println("successfully parsed: ")
        System.err.println(mediaUrl)
        System.err.println(mediaFilename)

        val downloaderTask = RTPPlayTSDownloaderTask(url, mediaUrl?:"", testDir.absolutePath, mediaFilename, defaultListener)
        downloaderTask.downloadMediaFile()
    }

    @Test
    fun tsfPlayer() {

        val url = "https://www.tsf.pt/programa/governo-sombra/arquivado-romano-e-totalmente-desastrado-12739725.html"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val isUrl : Boolean = NetworkUtils.isValidURL(url)

        if (!isUrl) throw RuntimeException("is not a valid website")

        val parsingTask = TSFParsingTask()
        parsingTask.parseMediaFile(url)

        val mediaUrl : String? = parsingTask.mediaUrl
        val mediaFilename : String = MediaUtils.getUniqueFilenameAndLock(testDir.absolutePath, parsingTask.filename ?: "")

        System.err.println("successfully parsed: ")
        System.err.println(mediaUrl)
        System.err.println(mediaFilename)

        val downloaderTask = DownloaderTask(mediaUrl?:"", testDir.absolutePath, mediaFilename, defaultListener)
        downloaderTask.downloadMediaFile()
    }
}