package org.hugoandrade.rtpplaydownloader.network.parsing

import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.SICParsingTaskV3
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.SICParsingTaskV4
import org.hugoandrade.rtpplaydownloader.network.utils.NetworkUtils
import org.junit.Assert
import org.junit.Test
import java.io.File

class ParsingUnitTestSICRadical : ParsingUnitTest() {

    @Test
    fun sicRadicalFromResourceHTML() {

        val filename = "SIC.Radical.20201218.Irritacoes.html";

        // get file from resources
        val url = javaClass.classLoader?.getResource(filename)?.file
                ?: return Assert.fail("failed to get file from resources")

        val parsingTask = SICParsingTaskV4()
        val parsed = parsingTask.parseMediaFile(File(url))

        System.err.println("successfully parsed ? " + parsed)

        debug(parsingTask)

        download(parsingTask)
    }

    @Test
    @Deprecated(message = "no longer valid")
    fun sicV3PlayerSICRadical() {

        val url = "https://sicradical.pt/programas/irritacoes/Videos/2020-09-11-Irritacoes---Programa-de-11-de-setembro"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val isUrl : Boolean = NetworkUtils.isValidURL(url)

        if (!isUrl) throw RuntimeException("is not a valid website")

        val parsingTask = SICParsingTaskV3()
        val parsed = parsingTask.parseMediaFile(url)

        System.err.println("successfully parsed ? " + parsed)

        debug(parsingTask)

        download(parsingTask)
    }
}