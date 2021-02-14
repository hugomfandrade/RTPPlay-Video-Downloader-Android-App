package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.utils.NetworkUtils
import org.jsoup.nodes.Document

open class ParsingTaskDelegate(private val parsingTasks : List<ParsingTask>)

    : ParsingTask() {

    override fun parseMediaFile(url: String): Boolean {

        this.url = url

        if (!NetworkUtils.isValidURL(url)) return false

        var selectedTask : ParsingTask? = null

        var doc : Document? = null

        for (task in parsingTasks) {

            // check if url is supported
            if (!task.isUrlSupported(url)) continue

            // initialize if still null
            doc = doc?: NetworkUtils.getDoc(url)

            // if null, return false
            if (doc == null) return false

            // if is able to parse, break out of loop with selected task
            if (task.parseMediaFile(doc)) {
                selectedTask = task
                break
            }
        }

        if (selectedTask == null) return false

        this.mediaUrl = selectedTask.mediaUrl
        this.filename = selectedTask.filename
        this.thumbnailUrl = selectedTask.thumbnailUrl

        return true
    }

    override fun isValid(url: String) : Boolean {

        var doc : Document? = null

        if (!NetworkUtils.isValidURL(url)) return false

        for (task in parsingTasks) {

            // check if url is supported
            if (!task.isUrlSupported(url)) continue

            // initialize if still null
            doc = doc?: NetworkUtils.getDoc(url)

            // if null, return false
            if (doc == null) return false

            // if is valid, return true
            if (task.isValid(doc)) return true
        }

        return true
    }

    override fun parseMediaUrl(doc: Document): String? {
        throw RuntimeException("delegate not defined")
    }

    // never called within class
    override fun parseMediaFileName(doc: Document): String {
        throw RuntimeException("delegate not defined")
    }
}