package org.hugoandrade.rtpplaydownloader.network.parsing.pagination

import java.util.function.Supplier

class PaginationIdentifier {

    init {
        throw AssertionError()
    }

    companion object {

        fun findHost(urlString: String): PaginationParserTask? {
            for (fileType: PaginationType in PaginationType.values()) {
                if (fileType.paginationTask.get().isValid(urlString)) {
                    return fileType.paginationTask.get()
                }
            }
            return null
        }
    }

    enum class PaginationType(val paginationTask : Supplier<PaginationParserTask>) {
        RTPPlay(Supplier { RTPPlayPaginationParserTask() }),
        SIC(Supplier { SICPaginationParserTask() })
    }
}