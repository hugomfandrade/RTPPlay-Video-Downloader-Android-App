package org.hugoandrade.rtpplaydownloader.network.parsing

import org.hugoandrade.rtpplaydownloader.utils.ListenableFutureImpl

class ParseFuture(val urlString : String): ListenableFutureImpl<ParsingData>() {

}