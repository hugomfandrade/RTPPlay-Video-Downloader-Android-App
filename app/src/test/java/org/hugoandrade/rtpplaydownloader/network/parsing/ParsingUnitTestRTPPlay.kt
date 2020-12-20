package org.hugoandrade.rtpplaydownloader.network.parsing

import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTask
import org.hugoandrade.rtpplaydownloader.network.download.RTPPlayTSDownloaderTask
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.RTPPlayParsingTask
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.RTPPlayParsingTaskV2
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.RTPPlayParsingTaskV3
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.hugoandrade.rtpplaydownloader.network.utils.NetworkUtils
import org.junit.Test

class ParsingUnitTestRTPPlay : ParsingUnitTest() {

    @Test
    fun rtpPlayV3Player() {

        val url =
        // "https://www.rtp.pt/play/p2064/gps"
        // "https://www.rtp.pt/play/p7701/operacao-shock-and-awe"
                // "https://www.rtp.pt/play/p7662/amor-de-improviso"
                "https://www.rtp.pt/play/p7684/operation-yellow-bird"

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
}