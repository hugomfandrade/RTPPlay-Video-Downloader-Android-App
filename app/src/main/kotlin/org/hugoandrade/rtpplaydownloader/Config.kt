package org.hugoandrade.rtpplaydownloader

interface Config {

    companion object {

        const val nParsingThreads: Int = 10
        const val nDownloadThreads: Int = 5
        const val nPersistenceThreads: Int = 5
        const val nImageLoadingThreads: Int = 10

        const val enablePauseResume = false
    }
}