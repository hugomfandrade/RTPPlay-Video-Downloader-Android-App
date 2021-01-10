package org.hugoandrade.rtpplaydownloader.network.parsing

import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.*
import org.hugoandrade.rtpplaydownloader.network.utils.NetworkUtils
import org.junit.Assert
import org.junit.Test
import java.io.File

class ParsingUnitTestRTPPlay : ParsingUnitTest() {

    @Test
    fun rtpPlayV5() {

        val url = "https://www.rtp.pt/play/p2064/gps"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val parsingTask = RTPPlayParsingTaskV5()
        val parsed = parsingTask.parseMediaFile(url)

        System.err.println("successfully parsed ? " + parsed)

        debug(parsingTask)

        download(parsingTask)
    }

    @Test
    @Deprecated(message = "no longer valid")
    fun rtpPlayV4() {

        val url = "https://www.rtp.pt/play/p2064/gps"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val parsingTask = RTPPlayParsingTaskV4()
        val parsed = parsingTask.parseMediaFile(url)

        System.err.println("successfully parsed ? " + parsed)

        debug(parsingTask)

        download(parsingTask)
    }

    @Test
    fun rtpPlayV4FromResourceHTML() {

        val filename = "RTPPlay.20201211.Ultimo.Apaga.a.Luz.html";

        // get file from resources
        val url = javaClass.classLoader?.getResource(filename)?.file
                ?: return Assert.fail("failed to get file from resources")

        val parsingTask = RTPPlayParsingTaskV4()
        val parsed = parsingTask.parseMediaFile(File(url))

        System.err.println("successfully parsed ? " + parsed)

        debug(parsingTask)

        download(parsingTask)
    }

    @Test
    @Deprecated(message = "no longer valid")
    fun rtpPlayV3Player() {

        val url = "https://www.rtp.pt/play/p2064/gps"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val isUrl : Boolean = NetworkUtils.isValidURL(url)

        if (!isUrl) throw RuntimeException("is not a valid website")

        val parsingTask = RTPPlayParsingTaskV3() // RTPPlayParsingTaskV3()
        val parsed = parsingTask.parseMediaFile(url)

        System.err.println("successfully parsed ? " + parsed)

        debug(parsingTask)

        download(parsingTask)
    }


    @Test
    @Deprecated(message = "no longer valid")
    fun rtpPlayV2Player() {

        val url = "https://www.rtp.pt/play/p2064/gps"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val isUrl : Boolean = NetworkUtils.isValidURL(url)

        if (!isUrl) throw RuntimeException("is not a valid website")

        val parsingTask = RTPPlayParsingTaskV2() // RTPPlayParsingTaskV2()
        val parsed = parsingTask.parseMediaFile(url)

        System.err.println("successfully parsed ? " + parsed)

        debug(parsingTask)

        download(parsingTask)
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
        val parsed = parsingTask.parseMediaFile(url)

        System.err.println("successfully parsed ? " + parsed)

        debug(parsingTask)

        download(parsingTask)
    }
}