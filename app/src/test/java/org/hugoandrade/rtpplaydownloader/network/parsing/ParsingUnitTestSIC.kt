package org.hugoandrade.rtpplaydownloader.network.parsing

import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTask
import org.hugoandrade.rtpplaydownloader.network.download.TSDownloaderTask
import org.hugoandrade.rtpplaydownloader.network.download.TSUtils
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.SICParsingTaskIdentifier
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.SICParsingTaskV1
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.SICParsingTaskV2
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.SICParsingTaskV3
import org.hugoandrade.rtpplaydownloader.network.utils.NetworkUtils
import org.junit.Test
import java.io.File

class ParsingUnitTestSIC : ParsingUnitTest() {

    @Test
    fun sicIdentifier_TS() {

        val tsUrl = "https://videos.impresa.pt/videos/hls/sicnot/2022/11/08/dacb13b8-e30c-4de1-8152-b73e7ae7cd26/futuro-hoje.1.m3u8"

        val parsingData = ParsingData(tsUrl, null, "nuada-futuro-hoje", null)

        val playlistURLs = TSUtils.getCompleteM3U8Playlist(tsUrl)
        val playlistURL = "https://videos.impresa.pt/videos/hls/sicnot/2022/11/08/dacb13b8-e30c-4de1-8152-b73e7ae7cd26/futuro-hoje.1-540p.m3u8"

        val task = TSDownloaderTask(playlistURL, File("test-download-folder").absolutePath, "nuada-futuro-hoje", defaultListener).downloadMediaFile()
        download(parsingData)
    }

    @Test
    fun sicIdentifier_20201226() {

        val url = "https://sicnoticias.pt/programas/eixodomal/2020-12-25-As-entrevistas-a-Cavaco-Silva-e-a-Passos-Coelho-e-o-massacre-de-animais-na-Azambuja"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val parsingTask = SICParsingTaskIdentifier()
        val parsed = parsingTask.parseMediaFile(url)

        debug(parsed)

        download(parsed)
    }

    @Test
    fun sicIdentifier_withTS() {

        val url = "https://sicnoticias.pt/programas/eixodomal/2020-09-11-Regresso-as-aulas-Presidenciais-e-a-morte-de-Vicente-Jorge-Silva.-O-Eixo-do-Mal-na-integra"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val parsingTask = SICParsingTaskIdentifier()
        val parsed = parsingTask.parseMediaFile(url)

        debug(parsed)

        download(parsed)
    }

    @Test
    fun sicIdentifier_withRaw() {

        val url = "https://sicradical.pt/programas/irritacoes/Videos/2020-12-19-Irritacoes---Programa-de-18-de-dezembro"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val isUrl : Boolean = NetworkUtils.isValidURL(url)

        if (!isUrl) throw RuntimeException("is not a valid website")

        val parsingTask = SICParsingTaskIdentifier()
        val parsed = parsingTask.parseMediaFile(url)

        debug(parsed)

        download(parsed)
    }

    @Test
    fun sicV3Player_withTSDownload() {

        val url = "https://sicnoticias.pt/programas/eixodomal/2020-09-11-Regresso-as-aulas-Presidenciais-e-a-morte-de-Vicente-Jorge-Silva.-O-Eixo-do-Mal-na-integra"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val isUrl : Boolean = NetworkUtils.isValidURL(url)

        if (!isUrl) throw RuntimeException("is not a valid website")

        val parsingTask = SICParsingTaskV3()
        val parsed = parsingTask.parseMediaFile(url)

        debug(parsed)

        download(parsed)
    }

    @Test
    @Deprecated(message = "no longer valid")
    fun sicV2Player() {

        val url = "https://sic.pt/Programas/governo-sombra/videos/2020-08-29-Governo-Sombra---28-de-agosto"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val isUrl : Boolean = NetworkUtils.isValidURL(url)

        if (!isUrl) throw RuntimeException("is not a valid website")

        val parsingTask = SICParsingTaskV2()
        val parsed = parsingTask.parseMediaFile(url)

        debug(parsed)

        download(parsed)
    }

    @Test
    @Deprecated(message = "no longer valid")
    fun sicPlayer() {

        val url = "https://sic.pt/Programas/governo-sombra/videos/2020-07-18-Governo-Sombra---17-de-julho"

        System.err.println("trying to parse: ")
        System.err.println(url)

        val isUrl : Boolean = NetworkUtils.isValidURL(url)

        if (!isUrl) throw RuntimeException("is not a valid website")

        val parsingTask = SICParsingTaskV1()
        val parsed = parsingTask.parseMediaFile(url)

        debug(parsed)

        download(parsed)
    }
}