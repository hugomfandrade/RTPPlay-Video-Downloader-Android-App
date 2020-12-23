package org.hugoandrade.rtpplaydownloader.network.parsing

import org.hugoandrade.rtpplaydownloader.network.download.RTPPlayTSDownloaderTask
import org.hugoandrade.rtpplaydownloader.network.download.RawDownloaderTask
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.RTPPlayParsingTask
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.RTPPlayParsingTaskV2
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.RTPPlayParsingTaskV3
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.hugoandrade.rtpplaydownloader.network.utils.NetworkUtils
import org.junit.Assert
import org.junit.Test
import java.io.File

class ParsingUnitTestRTPPlay : ParsingUnitTest() {

    @Test
    fun rtpPlayV4FromResourceHTML() {

        val filename = "RTPPlay.20201211.Ultimo.Apaga.a.Luz.html";

        // get file from resources
        val url = javaClass.classLoader?.getResource(filename)?.file
                ?: return Assert.fail("failed to get file from resources")

        val parsingTask = RTPPlayParsingTaskV3()
        val parsed = parsingTask.parseMediaFile(File(url))

        val mediaUrl : String? = parsingTask.mediaUrl
        val mediaFilename : String = MediaUtils.getUniqueFilenameAndLock(testDir.absolutePath, parsingTask.filename ?: "")

        System.err.println("successfully parsed ? " + parsed)
        System.err.println(mediaUrl)
        System.err.println(mediaFilename)

        val downloaderTask = RTPPlayTSDownloaderTask(url, mediaUrl?:"", testDir.absolutePath, mediaFilename, defaultListener)
        downloaderTask.downloadMediaFile()

    }

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

        val downloaderTask = RawDownloaderTask(mediaUrl?:"", testDir.absolutePath, mediaFilename, defaultListener)
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

        val downloaderTask = RawDownloaderTask(mediaUrl?:"", testDir.absolutePath, mediaFilename, defaultListener)
        downloaderTask.downloadMediaFile()
    }
}