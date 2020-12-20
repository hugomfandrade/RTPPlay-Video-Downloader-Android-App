package org.hugoandrade.rtpplaydownloader.network.parsing

import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTask
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.TSFParsingTask
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.hugoandrade.rtpplaydownloader.network.utils.NetworkUtils
import org.junit.Test

class ParsingUnitTestTSF : ParsingUnitTest() {

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