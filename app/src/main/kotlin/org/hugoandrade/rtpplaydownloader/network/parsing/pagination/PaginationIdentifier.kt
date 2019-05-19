package org.hugoandrade.rtpplaydownloader.network.parsing.pagination

class PaginationIdentifier() {

    init {
        throw AssertionError()
    }

    companion object {

        fun findHost(urlString: String): PaginationParserTaskBase? {
            for (fileType: PaginationType in PaginationType.values()) {
                if (fileType.paginationTask.isValid(urlString)) {
                    when (fileType) {
                        PaginationType.RTPPlay -> return RTPPlayPaginationParserTask()
                    }
                }
            }
            return null
        }
    }

    enum class PaginationType(val paginationTask : PaginationParserTaskBase) {
        RTPPlay(RTPPlayPaginationParserTask())
    }
}