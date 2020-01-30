package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

class ParsingIdentifier() {

    init {
        throw AssertionError()
    }

    companion object {

        fun findHost(url: String): ParsingTaskBase? {
            for (fileType: FileType in FileType.values()) {
                if (fileType.parsingTask.isValid(url)) {
                    return when (fileType) {
                        FileType.RTPPlayMultiPart -> RTPPlayParsingMultiPartTask()
                        FileType.RTPPlay -> RTPPlayV2ParsingTask()
                        FileType.SIC -> SICParsingTask()
                        FileType.SAPO -> SAPOParsingTask()
                        FileType.TVIPlayer -> TVIPlayerParsingTask()
                    }
                }
            }
            return null
        }

        fun findType(task: ParsingTaskBase): FileType? {
            if (task is RTPPlayParsingMultiPartTask) return FileType.RTPPlayMultiPart
            if (task is RTPPlayV2ParsingTask) return FileType.RTPPlay
            if (task is SICParsingTask) return FileType.SIC
            if (task is SAPOParsingTask) return FileType.SAPO
            if (task is TVIPlayerParsingTask) return FileType.TVIPlayer
            return null
        }

    }

    enum class FileType(var parsingTask: ParsingTaskBase) {
        RTPPlayMultiPart(RTPPlayParsingMultiPartTask()),
        RTPPlay(RTPPlayV2ParsingTask()),
        SIC(SICParsingTask()),
        SAPO(SAPOParsingTask()),
        TVIPlayer(TVIPlayerParsingTask())
    }
}