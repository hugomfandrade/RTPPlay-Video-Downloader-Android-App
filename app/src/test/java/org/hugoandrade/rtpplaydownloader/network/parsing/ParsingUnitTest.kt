package org.hugoandrade.rtpplaydownloader.network.parsing

import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.ParsingIdentifier
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.ParsingTask
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.SICParsingTask
import org.hugoandrade.rtpplaydownloader.network.utils.NetworkUtils
import org.junit.Test

import org.junit.Assert.*
import java.lang.RuntimeException

class ParsingUnitTest {

    @Test
    fun sicPlayer() {

        val sicUrl = "https://sic.pt/Programas/governo-sombra/videos/2020-08-29-Governo-Sombra---28-de-agosto"

        System.err.println("trying to parse: ")
        System.err.println(sicUrl)

        val isUrl : Boolean = NetworkUtils.isValidURL(sicUrl)

        if (!isUrl) {
            throw RuntimeException("is not a valid website")
        }
        // val parsingTask: ParsingTask = ParsingIdentifier.findHost(sicUrl) ?: throw RuntimeException("could not find host")

        // val parsing : Boolean = parsingTask.parseMediaFile(sicUrl)

        // if (!parsing) { throw RuntimeException("could not find filetype") }

        val parsingTask = SICParsingTask()

        val mediaUrl : String? = parsingTask.getVideoFile(sicUrl)

        System.err.println("successfully parsed: ")
        System.err.println(mediaUrl)
    }
}