package org.hugoandrade.rtpplaydownloader.network.parsing

import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.TSFParsingTask
import org.junit.Test

class ParsingUnitTestTSF : ParsingUnitTest() {

    @Test
    fun tsfPlayer_20201226() {

        val url = "https://www.tsf.pt/programa/governo-sombra/canceladores-implacaveis-democraturas-e-a-republica-socialista-portuguesa-13173399.html"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val parsingTask = TSFParsingTask()
        val parsed = parsingTask.parseMediaFile(url)

        System.err.println("successfully parsed ? " + parsed)

        debug(parsingTask)

        download(parsingTask)
    }

    @Test
    fun tsfPlayer() {

        val url = "https://www.tsf.pt/programa/governo-sombra/arquivado-romano-e-totalmente-desastrado-12739725.html"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val parsingTask = TSFParsingTask()
        val parsed = parsingTask.parseMediaFile(url)

        System.err.println("successfully parsed ? " + parsed)

        debug(parsingTask)

        download(parsingTask)
    }
}