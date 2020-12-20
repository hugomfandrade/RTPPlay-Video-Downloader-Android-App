package org.hugoandrade.rtpplaydownloader.network.parsing

import org.hugoandrade.rtpplaydownloader.network.download.TVIPlayerTSDownloaderTask
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.TVIPlayerParsingTask
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.hugoandrade.rtpplaydownloader.network.utils.NetworkUtils
import org.junit.Test

class ParsingUnitTestTVIPlayer : ParsingUnitTest() {

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
}