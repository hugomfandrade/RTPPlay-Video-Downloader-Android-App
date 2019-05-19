package org.hugoandrade.rtpplaydownloader.network.parsing

import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTaskBase
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationParserTaskBase

data class ParsingData(val task : DownloaderTaskBase, val paginationTask : PaginationParserTaskBase?)