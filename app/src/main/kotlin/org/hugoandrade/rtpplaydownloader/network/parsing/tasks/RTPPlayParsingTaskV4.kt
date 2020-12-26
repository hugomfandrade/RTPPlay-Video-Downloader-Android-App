package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.download.TSUtils
import org.hugoandrade.rtpplaydownloader.network.parsing.TSPlaylist

open class RTPPlayParsingTaskV4 : RTPPlayParsingTaskV3() {

    override fun parseM3U8Playlist(): TSPlaylist? {
        //
        val m3u8: String = mediaUrl ?: return null

        val playlist = TSUtils.getCompleteM3U8PlaylistWithoutBaseUrl(m3u8)

        // TODO
        // update mediaUrl fields for now for compatibility reasons
        mediaUrl = playlist?.getTSUrls()?.firstOrNull()?.url ?: mediaUrl

        return playlist
    }
}