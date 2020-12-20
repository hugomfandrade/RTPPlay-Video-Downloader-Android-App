package org.hugoandrade.rtpplaydownloader.network.parsing

import org.hugoandrade.rtpplaydownloader.network.utils.Predicate

class ParsingUtils

/**
 * Ensure this class is only used as a utility.
 */
private constructor() {

    init {
        throw AssertionError()
    }

    companion object {

        fun indexOfEx(string: String, subString: String): Int {
            if (string.contains(subString)) {
                return string.indexOf(subString) + subString.length
            }
            return 0
        }

        fun <T> findFirst(tasks : List<T>, predicate: Predicate<T>) : T? {
            for (task in tasks) {
                if (predicate.test(task)) return task
            }
            return null
        }
    }
}