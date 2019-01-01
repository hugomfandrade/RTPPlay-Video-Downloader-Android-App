package org.hugoandrade.rtpplaydownloader.network

final class FileIdentifier {

    constructor(){
        //throw AssertionError("cannot be instantiated");
    }

    fun findHost(urlString : String): FileType? {
        for (fileType : FileType in FileType.values()) {
            if (fileType.mDownloaderTask.isValid(urlString)) {
                return fileType;
            }
        }
        return null;
    }
}