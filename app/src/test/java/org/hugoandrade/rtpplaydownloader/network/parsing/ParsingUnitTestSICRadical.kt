package org.hugoandrade.rtpplaydownloader.network.parsing

import org.hugoandrade.rtpplaydownloader.network.download.SICTSDownloaderTask
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.SICParsingTaskV3
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.hugoandrade.rtpplaydownloader.network.utils.NetworkUtils
import org.junit.Test

class ParsingUnitTestSICRadical : ParsingUnitTest() {

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
}