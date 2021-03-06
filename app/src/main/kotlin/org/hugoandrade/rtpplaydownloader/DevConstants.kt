package org.hugoandrade.rtpplaydownloader

interface DevConstants {

    companion object {

        const val showLog: Boolean = false

        const val nParsingThreads: Int = 10
        const val nDownloadThreads: Int = 5
        const val nPersistenceThreads: Int = 5
        const val nImageLoadingThreads: Int = 10

        const val enablePauseResume = false

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
        private const val url_13 = "https://sicradical.pt/programas/irritacoes/Videos/2019-11-29-Irritacoes---Programa-de-29-de-novembro"
        private const val url_14 = "https://tviplayer.iol.pt/" +
                "programa/governo-sombra/53c6b3a33004dc006243d5fb/" +
                "video/5e07229c0cf20719306879c1";

        val url : String? = null
    }
}