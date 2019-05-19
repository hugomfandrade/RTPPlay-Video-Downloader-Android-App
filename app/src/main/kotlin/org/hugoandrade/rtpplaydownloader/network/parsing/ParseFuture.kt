package org.hugoandrade.rtpplaydownloader.network.parsing

import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTaskBase
import org.hugoandrade.rtpplaydownloader.utils.ListenableFutureImpl

class ParseFuture(val urtString : String): ListenableFutureImpl<ParsingData>() {

}