package org.hugoandrade.rtpplaydownloader.network.parsing.pagination

class PaginationIdentifier() {

    init {
        throw AssertionError()
    }

    companion object {

        fun findHost(urlString: String): PaginationParserTask? {
            for (fileType: PaginationType in PaginationType.values()) {
                if (fileType.paginationTask.isValid(urlString)) {
                    return when (fileType) {
                        PaginationType.RTPPlay -> RTPPlayPaginationParserTask()
                        PaginationType.SIC -> SICPaginationParserTask()
                    }
                }
            }
            return null
        }
    }

    enum class PaginationType(val paginationTask : PaginationParserTask) {
        RTPPlay(RTPPlayPaginationParserTask()),
        SIC(SICPaginationParserTask())
    }
}