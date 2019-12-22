package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

class EmptyParsingTask : ParsingTaskBase() {

    override fun getMediaFileName(url: String, videoFile: String?): String {

        return null.toString()
    }
    override fun parseMediaFile(url: String): Boolean {

        this.url = url
        return false
    }

    override fun isValid(urlString: String) : Boolean {

        return false
    }
}