package org.hugoandrade.rtpplaydownloader.network.parsing

import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.TVIPlayerParsingTask
import org.hugoandrade.rtpplaydownloader.network.utils.NetworkUtils
import org.junit.Test

class ParsingUnitTestTVIPlayer : ParsingUnitTest() {

    @Test
    fun tviPlayerPlayer_20201225() {

        val url = "https://tviplayer.iol.pt/programa/circulatura-do-quadrado/5c4b41730cf2a84eaefc024b/video/5fe3ea1f0cf2c7855558af33"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val isUrl : Boolean = NetworkUtils.isValidURL(url)

        if (!isUrl) throw RuntimeException("is not a valid website")

        val parsingTask = TVIPlayerParsingTask()
        val parsed = parsingTask.parseMediaFile(url)

        debug(parsed)

        download(parsed)
    }

    @Test
    fun tviPlayerPlayer() {

        // every URL
        val url = "https://tviplayer.iol.pt/programa/circulatura-do-quadrado/5c4b41730cf2a84eaefc024b"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val isUrl : Boolean = NetworkUtils.isValidURL(url)

        if (!isUrl) throw RuntimeException("is not a valid website")

        val parsingTask = TVIPlayerParsingTask()
        val parsed = parsingTask.parseMediaFile(url)

        debug(parsed)

        download(parsed)
    }
}