package org.hugoandrade.rtpplaydownloader.network.parsing.pagination

abstract class PaginationParserTask {

    var TAG : String = javaClass.simpleName

    private var isPaginationCompleted = true

    abstract fun isValid(urlString: String) : Boolean
    abstract fun parsePagination(urlString: String) : ArrayList<String>
    open fun parseMore() : ArrayList<String> {
        return ArrayList()
    }

    fun getPaginationComplete() : Boolean {
        return isPaginationCompleted
    }

    fun setPaginationComplete(isCompleted: Boolean) {
        isPaginationCompleted = isCompleted
    }
}