package org.hugoandrade.rtpplaydownloader.network.parsing.pagination

abstract class PaginationParserTaskBase {

    var TAG : String = javaClass.simpleName

    abstract fun isValid(urlString: String) : Boolean
}