package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

class ParsingIdentifier() {

    init {
        throw AssertionError()
    }

    companion object {

        fun findHost(url: String): ParsingTask? {
            for (fileType: FileType in FileType.values()) {
                if (fileType.parsingTask.isValid(url)) {
                    return when (fileType) {
                        // search for multi-part before rtp play
                        FileType.RTPPlayMultiPart -> RTPPlayParsingMultiPartTask()
                        FileType.RTPPlay -> RTPPlayParsingTaskCompat()
                        FileType.SIC -> SICParsingTaskCompat()
                        FileType.SAPO -> SAPOParsingTask()
                        FileType.TVIPlayer -> TVIPlayerParsingTask()
                    }
                }
            }
            return null
        }

        fun findType(task: ParsingTask): FileType? {
            if (task is RTPPlayParsingMultiPartTask) return FileType.RTPPlayMultiPart
            if (task is RTPPlayParsingTask ||
                    task is RTPPlayParsingTaskV2 ||
                    task is RTPPlayParsingTaskV3 ||
                    task is RTPPlayParsingTaskCompat) return FileType.RTPPlay
            if (task is SICParsingTask
                    || task is SICParsingTaskV2
                    || task is SICParsingTaskCompat) return FileType.SIC
            if (task is SAPOParsingTask) return FileType.SAPO
            if (task is TVIPlayerParsingTask) return FileType.TVIPlayer
            return null
        }

    }

    enum class FileType(var parsingTask: ParsingTask) {
        RTPPlayMultiPart(RTPPlayParsingMultiPartTask()),
        RTPPlay(RTPPlayParsingTaskCompat()),
        SIC(SICParsingTaskCompat()),
        SAPO(SAPOParsingTask()),
        TVIPlayer(TVIPlayerParsingTask())
    }
}