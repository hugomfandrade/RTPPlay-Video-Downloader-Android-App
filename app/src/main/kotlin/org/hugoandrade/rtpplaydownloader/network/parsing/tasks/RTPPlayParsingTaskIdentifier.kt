package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingUtils
import org.hugoandrade.rtpplaydownloader.network.utils.Predicate
import org.jsoup.nodes.Document
import java.lang.RuntimeException

class RTPPlayParsingTaskIdentifier : ParsingTask() {


    private val parsingTasks = listOf(
            RTPPlayParsingTaskV3(),
            RTPPlayParsingTaskV2(),
            RTPPlayParsingTask()
    )

    override fun parseMediaFile(url: String): Boolean {

        this.url = url

        val task : ParsingTask = findFirst(Predicate{ task -> task.parseMediaFile(url) })?: return false

        this.mediaUrl = task.mediaUrl
        this.filename = task.filename
        this.thumbnailUrl = task.thumbnailUrl

        return true
    }

    override fun isValid(url: String) : Boolean {
        findFirst(Predicate{ task -> task.isValid(url) }) ?: return false
        return true
    }

    private fun findFirst(predicate: Predicate<ParsingTask>) : ParsingTask? {
        return ParsingUtils.findFirst(parsingTasks, predicate)
    }

    override fun getMediaUrl(doc: Document): String? {
        throw RuntimeException("delegated not defined")
    }

    // never called within class
    override fun getMediaFileName(doc: Document): String {
        throw RuntimeException("delegated not defined")
    }
}