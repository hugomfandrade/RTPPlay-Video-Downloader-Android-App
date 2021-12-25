package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.download.TSUtils
import org.hugoandrade.rtpplaydownloader.network.parsing.TSPlaylist

@Deprecated(message = "use a more recent RTPPlay parser")
open class RTPPlayParsingTaskV4 : RTPPlayParsingTaskV3() {

    override fun parseM3U8Playlist(m3u8: String): TSPlaylist? {
        return TSUtils.getCompleteM3U8PlaylistWithoutBaseUrl(m3u8)
    }
}