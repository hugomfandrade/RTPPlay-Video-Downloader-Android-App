package org.hugoandrade.rtpplaydownloader.network.parsing

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
    }
}