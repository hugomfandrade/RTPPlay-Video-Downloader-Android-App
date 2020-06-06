package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

class EmptyParsingTask : ParsingTask() {

    override fun getMediaFileName(url: String, videoFile: String?): String {

        return null.toString()
    }
    override fun parseMediaFile(url: String): Boolean {

        this.url = url
        return false
    }

    override fun isValid(url: String) : Boolean {

        return false
    }
}