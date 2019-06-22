package org.hugoandrade.rtpplaydownloader.network.utils

class FilenameLockerAdapter {

    companion object {
        val instance = FilenameLockerAdapter()
    }

    private val filenames : HashSet<String> = HashSet()
    private val unremovableFilenames : HashSet<String> = HashSet()

    fun contains(filename : String):Boolean {
        return unremovableFilenames.contains(filename) || filenames.contains(filename)
    }

    fun put(filename : String):Boolean {
        if (unremovableFilenames.contains(filename)) {
            return false
        }
        return filenames.add(filename)
    }

    fun remove(filename : String):Boolean {
        return filenames.remove(filename)
    }

    fun clear() {
        filenames.clear()
    }

    fun putUnremovable(filename : String) : Boolean {
        return unremovableFilenames.add(filename)
    }
}