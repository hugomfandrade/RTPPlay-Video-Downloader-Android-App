package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.dev.parsing.DevParsingTask
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingData
import org.hugoandrade.rtpplaydownloader.network.utils.NetworkUtils
import org.jsoup.nodes.Document
import java.util.function.Supplier

class ParsingIdentifier {

    init {
        throw AssertionError()
    }

    companion object {

        fun findHost(url: String?): ParsingTask? {

            if (url == null) return null

            val doc : Document = NetworkUtils.getDoc(url) ?:
                /* return null */Document(url)

            for (fileType: FileType in FileType.values()) {

                if (fileType.parsingTask.get().isValid(doc)) {
                    return fileType.parsingTask.get()
                }
            }
            return null
        }

        fun findType(task: ParsingTask?): FileType? {

            for (fileType: FileType in FileType.values()) {
                val taskClass =fileType.parsingTask.get().javaClass
                if (task?.javaClass ==  taskClass) {
                    return fileType
                }
            }
            return null
        }

        fun findType(data: ParsingData?): FileType? {
            if (data == null) return null
            val task : ParsingTask? = findHost(data.url)
            return findType(task)
        }
    }

    enum class FileType(var parsingTask: Supplier<ParsingTask>) {
        // search for multi-part before rtp play
        RTPPlayMultiPart(Supplier { RTPPlayParsingMultiPartTask() }),
        RTPPlay(Supplier { RTPPlayParsingTaskIdentifier() }),

        SIC(Supplier { SICParsingTaskIdentifier() }),
        SAPO(Supplier { SAPOParsingTask() }),
        TVIPlayer(Supplier { TVIPlayerParsingTask() }),
        TSF(Supplier { TSFParsingTask() }),

        DEV(Supplier { DevParsingTask() })
    }
}