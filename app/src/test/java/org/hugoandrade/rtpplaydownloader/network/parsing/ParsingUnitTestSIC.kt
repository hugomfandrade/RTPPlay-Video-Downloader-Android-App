package org.hugoandrade.rtpplaydownloader.network.parsing

import org.hugoandrade.rtpplaydownloader.network.DownloadableItem
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderIdentifier
import org.hugoandrade.rtpplaydownloader.network.download.RawDownloaderTask
import org.hugoandrade.rtpplaydownloader.network.download.SICTSDownloaderTask
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.*
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.hugoandrade.rtpplaydownloader.network.utils.NetworkUtils
import org.junit.Test

class ParsingUnitTestSIC : ParsingUnitTest() {

    @Test
    fun sicCompat() {

        val url =
                // "https://sicnoticias.pt/programas/eixodomal/2020-09-11-Regresso-as-aulas-Presidenciais-e-a-morte-de-Vicente-Jorge-Silva.-O-Eixo-do-Mal-na-integra"
                "https://sicradical.pt/programas/irritacoes/Videos/2020-10-03-Irritacoes---Programa-de-2-de-outubro"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val isUrl : Boolean = NetworkUtils.isValidURL(url)

        if (!isUrl) throw RuntimeException("is not a valid website")

        val parsingTask = SICParsingTaskIdentifier()
        if (!parsingTask.parseMediaFile(url)) throw RuntimeException("failed to parse media file")

        val mediaUrl : String = parsingTask.mediaUrl ?: throw RuntimeException("could not parse media file")
        val thumbnailUrl : String? = parsingTask.thumbnailUrl
        val filename : String? = parsingTask.filename

        System.err.println("successfully parsed: $mediaUrl")

        val item = DownloadableItem(url = url, mediaUrl = mediaUrl, thumbnailUrl = thumbnailUrl, filename = filename)
        item.downloadTask = ParsingIdentifier.findType(parsingTask)?.name

        val downloaderTask = DownloaderIdentifier.findTask(testDir.absolutePath, item, defaultListener)

        System.err.println("about to download: ${downloaderTask.javaClass.simpleName}")

        downloaderTask.downloadMediaFile()
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

        System.err.println("successfully parsed: ")
        System.err.println(mediaUrl)

        val tsPlaylist = parsingTask.getTSPlaylist()

        System.err.println("ts Urls are: ")
        System.err.println(tsPlaylist?.getTSUrls())


        if (true) return
        val mediaFilename : String = MediaUtils.getUniqueFilenameAndLock(testDir.absolutePath, parsingTask.filename ?: "")

        System.err.println(mediaFilename)

        val sicTSDownloaderTask = SICTSDownloaderTask(url, mediaUrl?:"", testDir.absolutePath, mediaFilename, defaultListener)
        sicTSDownloaderTask.downloadMediaFile()
    }

    @Test
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

        val sicTSDownloaderTask = RawDownloaderTask(mediaUrl?:"", testDir.absolutePath, mediaFilename, defaultListener)
        sicTSDownloaderTask.downloadMediaFile()
    }
}