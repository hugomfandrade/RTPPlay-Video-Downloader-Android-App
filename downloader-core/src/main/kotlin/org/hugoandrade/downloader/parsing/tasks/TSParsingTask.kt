package org.hugoandrade.downloader.parsing.tasks

import org.hugoandrade.downloader.parsing.ParsingData
import org.hugoandrade.downloader.parsing.TSPlaylist
import org.jsoup.nodes.Document

interface TSParsingTask : ParsingTask {

    override fun parseMediaFile(doc: Document): ParsingData? {
        val parsingData : ParsingData = super.parseMediaFile(doc) ?: return null

        val playlist = parsingData.mediaUrl?.let { parseM3U8Playlist(it) }

        parsingData.m3u8Playlist = playlist

        return if (playlist == null) null else parsingData
    }

    fun parseM3U8Playlist(m3u8: String): TSPlaylist?
}