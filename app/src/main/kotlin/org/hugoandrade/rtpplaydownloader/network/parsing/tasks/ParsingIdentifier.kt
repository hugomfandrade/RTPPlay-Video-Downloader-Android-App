package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingData
import org.hugoandrade.rtpplaydownloader.network.utils.NetworkUtils
import org.jsoup.nodes.Document

class ParsingIdentifier {

    init {
        throw AssertionError()
    }

    companion object {

        fun findHost(url: String?): ParsingTask? {

            if (url == null) return null

            for (fileType: FileType in FileType.values()) {

                val doc : Document = NetworkUtils.getDoc(url) ?: return null

                if (fileType.parsingTask.isValid(doc)) {
                    return when (fileType) {
                        // search for multi-part before rtp play
                        FileType.RTPPlayMultiPart -> RTPPlayParsingMultiPartTask()
                        FileType.RTPPlay -> RTPPlayParsingTaskIdentifier()
                        FileType.SIC -> SICParsingTaskIdentifier()
                        FileType.SAPO -> SAPOParsingTask()
                        FileType.TVIPlayer -> TVIPlayerParsingTask()
                        FileType.TSF -> TSFParsingTask()
                    }
                }
            }
            return null
        }

        fun findType(task: ParsingTask?): FileType? {
            if (task is RTPPlayParsingMultiPartTask) return FileType.RTPPlayMultiPart
            if (task is RTPPlayParsingTaskIdentifier) return FileType.RTPPlay
            if (task is SICParsingTaskIdentifier) return FileType.SIC
            if (task is SAPOParsingTask) return FileType.SAPO
            if (task is TVIPlayerParsingTask) return FileType.TVIPlayer
            if (task is TSFParsingTask) return FileType.TSF
            return null
        }

        fun findType(data: ParsingData?): FileType? {
            if (data == null) return null
            val task : ParsingTask? = findHost(data.url)
            return findType(task)
        }
    }

    enum class FileType(var parsingTask: ParsingTask) {
        RTPPlayMultiPart(RTPPlayParsingMultiPartTask()),
        RTPPlay(RTPPlayParsingTaskIdentifier()),
        SIC(SICParsingTaskIdentifier()),
        SAPO(SAPOParsingTask()),
        TVIPlayer(TVIPlayerParsingTask()),
        TSF(TSFParsingTask())
    }
}