package org.hugoandrade.rtpplaydownloader.network

final class FileIdentifier {

    constructor(){
        //throw AssertionError("cannot be instantiated");
    }

    fun findHost(urlString : String): DownloaderTaskBase? {
        for (fileType : FileType in FileType.values()) {
            if (fileType.mDownloaderTask.isValid(urlString)) {
                when (fileType) {
                    FileType.RTPPlay -> return RTPPlayDownloaderTask()
                }
            }
        }
        return null;
    }
}