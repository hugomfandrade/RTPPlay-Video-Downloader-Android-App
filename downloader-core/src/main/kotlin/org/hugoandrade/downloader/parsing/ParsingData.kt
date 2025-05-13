package org.hugoandrade.downloader.parsing

data class ParsingData(var url: String?,
                       var mediaUrl: String?,
                       var filename: String?,
                       var thumbnailUrl: String?) {

    var m3u8Playlist: TSPlaylist? = null
}