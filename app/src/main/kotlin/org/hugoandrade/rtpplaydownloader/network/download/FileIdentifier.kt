package org.hugoandrade.rtpplaydownloader.network.download

class FileIdentifier() {

    init {
        throw AssertionError()
    }

    companion object {

        fun findHost(url: String): DownloaderTaskBase? {
            for (fileType: FileType in FileType.values()) {
                if (fileType.downloaderTask.isValid(url)) {
                    when (fileType) {
                        FileType.RTPPlayMultiPart -> return RTPPlayDownloaderMultiPartTask()
                        FileType.RTPPlay -> return RTPPlayDownloaderTask()
                        FileType.SIC -> return SICDownloaderTask()
                        FileType.SAPO -> return SAPODownloaderTask()
                    }
                }
            }
            return null
        }

        fun findHostForSingleTask(url: String): DownloaderTaskBase? {
            for (fileType: FileType in FileType.values()) {
                if (fileType.downloaderTask.isValid(url)) {
                    when (fileType) {
                        FileType.RTPPlay -> return RTPPlayDownloaderTask()
                        FileType.SIC -> return SICDownloaderTask()
                        FileType.SAPO -> return SAPODownloaderTask()
                    }
                }
            }
            return null
        }
    }

    enum class FileType(var downloaderTask: DownloaderTaskBase) {
        RTPPlayMultiPart(RTPPlayDownloaderMultiPartTask()),
        RTPPlay(RTPPlayDownloaderTask()),
        SIC(SICDownloaderTask()),
        SAPO(SAPODownloaderTask())
    }
}