package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.utils.NetworkUtils
import org.jsoup.nodes.Document

open class ParsingTaskDelegate(private val parsingTasks : List<ParsingTask>) : ParsingTask() {

    override fun isUrlSupported(url: String) : Boolean {

        for (task in parsingTasks) {
            if (task.isUrlSupported(url)) return true
        }

        return false
    }

    override fun parseMediaFile(doc: Document): Boolean {

        val url = doc.baseUri()

        if (!NetworkUtils.isValidURL(url)) return false

        var selectedTask : ParsingTask? = null

        for (task in parsingTasks) {

            // check if url is supported
            if (!task.isUrlSupported(url)) continue

            // if is able to parse, break out of loop with selected task
            if (task.parseMediaFile(doc)) {
                selectedTask = task
                break
            }
        }

        if (selectedTask == null) return false

        this.url = selectedTask.url
        this.mediaUrl = selectedTask.mediaUrl
        this.filename = selectedTask.filename
        this.thumbnailUrl = selectedTask.thumbnailUrl

        return true
    }

    override fun isValid(doc: Document) : Boolean {

        val url = doc.baseUri()

        if (!NetworkUtils.isValidURL(url)) return false

        for (task in parsingTasks) {

            // check if url is supported
            if (!task.isUrlSupported(url)) continue

            // if is valid, return true
            if (task.isValid(doc)) return true
        }

        return false
    }

    override fun parseMediaUrl(doc: Document): String? {
        throw RuntimeException("delegate not defined")
    }

    // never called within class
    override fun parseMediaFileName(doc: Document): String {
        throw RuntimeException("delegate not defined")
    }
}