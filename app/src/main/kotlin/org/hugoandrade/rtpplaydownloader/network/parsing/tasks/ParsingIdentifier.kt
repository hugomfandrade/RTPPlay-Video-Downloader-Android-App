package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

class ParsingIdentifier() {

    init {
        throw AssertionError()
    }

    companion object {

        fun findHost(url: String): ParsingTaskBase? {
            for (fileType: FileType in FileType.values()) {
                if (fileType.parsingTask.isValid(url)) {
                    when (fileType) {
                        FileType.RTPPlayMultiPart -> return RTPPlayParsingMultiPartTask()
                        FileType.RTPPlay -> return RTPPlayParsingTask()
                        FileType.SIC -> return SICParsingTask()
                        FileType.SAPO -> return SAPOParsingTask()
                        FileType.TVIPlayer -> return TVIPlayerParsingTask()
                    }
                }
            }
            return null
        }

        fun findType(task: ParsingTaskBase): FileType? {
            if (task is RTPPlayParsingMultiPartTask) return FileType.RTPPlayMultiPart
            if (task is RTPPlayParsingTask) return FileType.RTPPlay
            if (task is SICParsingTask) return FileType.SIC
            if (task is SAPOParsingTask) return FileType.SAPO
            if (task is TVIPlayerParsingTask) return FileType.TVIPlayer
            return null
        }

    }

    enum class FileType(var parsingTask: ParsingTaskBase) {
        RTPPlayMultiPart(RTPPlayParsingMultiPartTask()),
        RTPPlay(RTPPlayParsingTask()),
        SIC(SICParsingTask()),
        SAPO(SAPOParsingTask()),
        TVIPlayer(TVIPlayerParsingTask())
    }
}