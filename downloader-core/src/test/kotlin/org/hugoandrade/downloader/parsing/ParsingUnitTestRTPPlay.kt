package org.hugoandrade.downloader.parsing

import org.hugoandrade.downloader.download.TSDownloaderTask
import org.hugoandrade.downloader.download.TSUtils
import org.hugoandrade.downloader.parsing.tasks.*
import org.hugoandrade.downloader.utils.NetworkUtils
import org.jsoup.Jsoup
import org.junit.Assert
import org.junit.Test
import java.io.File

class ParsingUnitTestRTPPlay : ParsingUnitTest() {

    @Test
    fun m3u8() {
        val url = "https://videos-fms.jwpsrv.com/0_623f986d_0x92a1c47902525b50bef8f5357e1dd566753de399/content/conversions/i0kOV6po/videos/K0BIshL4-34192113.m4a.m3u8"

        val downloaderTask = TSDownloaderTask(url, testDir.absolutePath, "name.m3u8", defaultListener, object :
            TSUtils.Validator<String> {
            override fun isValid(o: String): Boolean {
                return o.contains(".aac")
            }
        })

        System.err.println("about to download: ${downloaderTask.javaClass.simpleName}")

        downloaderTask.run()
    }

    @Test
    fun rtpPlayV8_02() {
        DO_DOWNLOAD = true

        val url =
            "https://www.rtp.pt/play/p11356/e670466/freddie-the-final-act"
            // "https://www.rtp.pt/play/p10209/3-mulheres-pos-revolucao"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val parsingTask = ParsingIdentifier.findHost(url) ?: return
        val parsed = parsingTask.parseMediaFile(url)

        debug(parsed)

        download(parsed)
    }

    @Test
    fun rtpPlayV8() {
        DO_DOWNLOAD = true

        val url = "https://www.rtp.pt/play/p8306/e540074/o-outro-lado"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val parsingTask = RTPPlayParsingTaskV8()
        val parsed = parsingTask.parseMediaFile(url)

        debug(parsed)

        download(parsed)
    }

    @Test
    fun rtpPlayAudio() {

        val url = "https://www.rtp.pt/play/p5661/vamos-todos-morrer"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val parsingTask = RTPPlayParsingTaskIdentifier()
        val parsed = parsingTask.parseMediaFile(url)

        debug(parsed)

        download(parsed)
    }

    @Test
    fun rtpPlayV5Audio() {

        val url = "https://www.rtp.pt/play/p5661/vamos-todos-morrer"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val parsingTask = RTPPlayParsingTaskV5()
        val parsed = parsingTask.parseMediaFile(url)

        debug(parsed)

        download(parsed)
    }

    @Test
    fun rtpPlayV4Audio() {

        val url = "https://www.rtp.pt/play/p5661/vamos-todos-morrer"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val parsingTask = RTPPlayParsingTaskV4()
        val parsed = parsingTask.parseMediaFile(url)

        debug(parsed)

        download(parsed)
    }

    @Test
    fun rtpPlayV7() {

        // val url = "https://www.rtp.pt/play/p2064/gps"
        val url = "https://www.rtp.pt/play/p8550/trumps-american-carnage"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val parsingTask = RTPPlayParsingTaskV7()
        val parsed = parsingTask.parseMediaFile(url)

        debug(parsed)

        download(parsed)
    }

    @Test
    @Deprecated(message = "no longer valid")
    fun rtpPlayV6() {

        val url = "https://www.rtp.pt/play/p2064/gps"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val parsingTask = org.hugoandrade.downloader.parsing.tasks.RTPPlayParsingTaskV6()
        val parsed = parsingTask.parseMediaFile(url)

        debug(parsed)

        download(parsed)
    }

    @Test
    @Deprecated(message = "no longer valid")
    fun rtpPlayV5() {

        val url = "https://www.rtp.pt/play/p2064/gps"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val parsingTask = RTPPlayParsingTaskV5()
        val parsed = parsingTask.parseMediaFile(url)

        debug(parsed)

        download(parsed)
    }

    @Test
    @Deprecated(message = "no longer valid")
    fun rtpPlayV4() {

        val url = "https://www.rtp.pt/play/p2064/gps"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val parsingTask = RTPPlayParsingTaskV4()
        val parsed = parsingTask.parseMediaFile(url)

        debug(parsed)

        download(parsed)
    }

    @Test
    fun rtpPlayV4FromResourceHTML() {

        val filename = "RTPPlay.20201211.Ultimo.Apaga.a.Luz.html";

        // get file from resources
        val url = javaClass.classLoader?.getResource(filename)?.file
                ?: return Assert.fail("failed to get file from resources")

        val file = File(url)
        val doc = Jsoup.parse(file, null)

        val parsingTask = RTPPlayParsingTaskV4()
        val parsed = parsingTask.parseMediaFile(doc)

        debug(parsed)

        download(parsed)
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

        debug(parsed)

        download(parsed)
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
        val parsed = parsingTask.parseMediaFile(url)

        debug(parsed)

        download(parsed)
    }

    @Test
    @Deprecated(message = "no longer valid")
    fun rtpPlayV1Player() {

        val url = "https://www.rtp.pt/play/p2064/gps"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val isUrl : Boolean = NetworkUtils.isValidURL(url)

        if (!isUrl) throw RuntimeException("is not a valid website")

        val parsingTask = RTPPlayParsingTaskV1()
        val parsed = parsingTask.parseMediaFile(url)

        debug(parsed)

        download(parsed)
    }
}