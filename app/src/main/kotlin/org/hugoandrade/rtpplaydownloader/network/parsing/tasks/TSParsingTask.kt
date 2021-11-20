package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.parsing.TSPlaylist
import org.jsoup.nodes.Document

abstract class TSParsingTask : ParsingTask() {

    var playlist: TSPlaylist? = null

    override fun parseMediaFile(doc: Document): Boolean {
        val parsed = super.parseMediaFile(doc)

        playlist = parseM3U8Playlist()

        return playlist != null
    }

    abstract fun parseM3U8Playlist(): TSPlaylist?

    fun getTSPlaylist() : TSPlaylist? {
        return playlist
    }
}