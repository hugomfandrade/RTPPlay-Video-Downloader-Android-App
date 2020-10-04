package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

class SICParsingTaskCompat : ParsingTask() {

    private val v1 = SICParsingTask()
    private val v2 = SICParsingTaskV2()
    private val v3 = SICParsingTaskV3()
    private val v4 = SICParsingTaskV4()

    override fun parseMediaFile(url: String): Boolean {

        this.url = url

        val task : ParsingTask = when {
            v4.parseMediaFile(url) -> v4
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

        if (v4.isValid(url)) return true
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