package org.hugoandrade.rtpplaydownloader.network.parsing

interface TSParsingTask {

    fun getTSPlaylist() : TSPlaylist?
}