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
                    }
                }
            }
            return null
        }

    }

    enum class FileType(var parsingTask: ParsingTaskBase) {
        RTPPlayMultiPart(RTPPlayParsingMultiPartTask()),
        RTPPlay(RTPPlayParsingTask()),
        SIC(SICParsingTask()),
        SAPO(SAPOParsingTask())
    }
}