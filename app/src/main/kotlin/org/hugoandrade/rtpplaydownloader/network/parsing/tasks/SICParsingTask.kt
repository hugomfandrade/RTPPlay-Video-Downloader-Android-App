package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

abstract class SICParsingTask : ParsingTask() {

    override fun isUrlSupported(url: String): Boolean {

        return url.contains("sicradical.sapo.pt") ||
                url.contains("sicradical.pt") ||
                url.contains("sicnoticias.sapo.pt") ||
                url.contains("sicnoticias.pt") ||
                url.contains("sic.sapo.pt") ||
                url.contains("sic.pt")
    }
}