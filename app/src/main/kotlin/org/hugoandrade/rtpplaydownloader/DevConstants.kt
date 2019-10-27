package org.hugoandrade.rtpplaydownloader

import android.content.Context

interface DevConstants {

    companion object {

        const val simDownload: Boolean = false
        const val showLog: Boolean = false

        const val nPersistenceThreads: Int = 5

        const val nParsingThreads: Int = 10
        const val nDownloadThreads: Int = 5

        const val enablePauseResume = false
        const val enablePersistence = true

        private const val url_1 = "https://www.rtp.pt/play/p2383/e236098/aqui-tao-longe"
        private const val url_2 = "https://www.rtp.pt/play/p5407/barao-negro"
        private const val url_3 = "https://www.rtp.pt/play/p5682/bad-breakfast"
        private const val url_4 = "https://www.rtp.pt/play/p5421/teorias-da-conspiracao"
        private const val url_5 = "https://sicnoticias.pt/programas/verdade-ou-consequencia/2019-03-31-Verdade-ou-Consequencia-com-Adolfo-Mesquita-Nunes"
        private const val url_6 = "https://www.rtp.pt/play/p4257/o-outro-lado"
        private const val url_7 = "https://www.rtp.pt/play/p5683/menos-um"
        private const val url_8 = "http://videos.sapo.pt/30CEmMITz50Tizli6EYv"
        private const val url_9 = "https://sic.pt/Programas/e-se-fosse-consigo/2019-05-03-E-se-fosse-consigo---brevemente-na-SIC"
        private const val url_10 = "https://www.rtp.pt/play/p5986/a-revolucao-silenciosa"
        private const val url_11 = "https://www.rtp.pt/play/p5093/perdidos"
        private const val url_12 = "https://sicradical.pt/programas/cabaret-da-coxa/episodios"

        val url : String? = url_7
    }
}