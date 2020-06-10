package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

class RTPPlayParsingTaskCompat : ParsingTask() {

    val v1 = RTPPlayParsingTask()
    val v2 = RTPPlayParsingTaskV2()
    val v3 = RTPPlayParsingTaskV3()

    override fun parseMediaFile(url: String): Boolean {

        this.url = url

        val task : ParsingTask = when {
            v3.parseMediaFile(url) -> v3
            v2.parseMediaFile(url) -> v2
            v1.parseMediaFile(url) -> v1
            else -> return false
        }

        this.mediaUrl = task.mediaUrl
        this.filename = task.filename
        this.thumbnailUrl = task.thumbnailUrl

        return true
    }

    override fun isValid(url: String) : Boolean {

        if (v3.isValid(url)) return true
        if (v2.isValid(url)) return true
        if (v1.isValid(url)) return true

        return false
    }

    // never called within class
    override fun getMediaFileName(url: String, videoFile: String?): String {
        return RTPPlayUtils.getMediaFileName(url, videoFile)
    }
}