package org.hugoandrade.rtpplaydownloader.network.parsing

import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTask
import org.hugoandrade.rtpplaydownloader.network.download.SICTSDownloaderTask
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.SICParsingTask
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.SICParsingTaskV2
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.SICParsingTaskV3
import org.hugoandrade.rtpplaydownloader.network.utils.NetworkUtils
import org.junit.Test
import java.io.File

class ParsingUnitTest {

    @Test
    fun sicV2Player() {

        val sicUrl = "https://sic.pt/Programas/governo-sombra/videos/2020-08-29-Governo-Sombra---28-de-agosto"

        System.err.println("trying to parse: ")
        System.err.println(sicUrl)

        val isUrl : Boolean = NetworkUtils.isValidURL(sicUrl)

        if (!isUrl) {
            throw RuntimeException("is not a valid website")
        }
        // val parsingTask: ParsingTask = ParsingIdentifier.findHost(sicUrl) ?: throw RuntimeException("could not find host")

        // val parsing : Boolean = parsingTask.parseMediaFile(sicUrl)

        // if (!parsing) { throw RuntimeException("could not find filetype") }

        val parsingTask = SICParsingTaskV2()

        val mediaUrl : String? = parsingTask.getVideoFile(sicUrl)
        val mediaFilename : String? = parsingTask.getMediaFileName(sicUrl, mediaUrl)

        System.err.println("successfully parsed: ")
        System.err.println(mediaUrl)
        System.err.println(mediaFilename)
    }

    @Test
    fun sicV3Player() {

        val sicUrl = "https://sicnoticias.pt/programas/eixodomal/2020-09-11-Regresso-as-aulas-Presidenciais-e-a-morte-de-Vicente-Jorge-Silva.-O-Eixo-do-Mal-na-integra"

        System.err.println("trying to parse: ")
        System.err.println(sicUrl)

        val isUrl : Boolean = NetworkUtils.isValidURL(sicUrl)

        if (!isUrl) {
            throw RuntimeException("is not a valid website")
        }

        val parsingTask = SICParsingTaskV3()

        val mediaUrl : String? = parsingTask.getVideoFile(sicUrl)
        val mediaFilename : String? = parsingTask.getMediaFileName(sicUrl, mediaUrl)

        System.err.println("successfully parsed: ")
        System.err.println(mediaUrl)
        System.err.println(mediaFilename)

        val sicTSDownloaderTask = SICTSDownloaderTask(sicUrl, mediaUrl?:"", "", mediaFilename?:"", object : DownloaderTask.Listener {
            override fun downloadStarted(f: File) {
                System.err.println("downloadStarted " + f)
            }

            override fun onProgress(downloadedSize: Long, totalSize: Long) {
                System.err.println("onProgress " + downloadedSize + " - " + totalSize)
            }

            override fun downloadFinished(f: File) {
                System.err.println("downloadFinished " + f)
            }

            override fun downloadFailed(message: String?) {
                System.err.println("downloadFailed " + message)
            }
        })
        sicTSDownloaderTask.downloadMediaFile()
    }
}