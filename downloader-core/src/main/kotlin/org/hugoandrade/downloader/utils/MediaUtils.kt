package org.hugoandrade.downloader.utils

import java.io.File
import java.text.Normalizer
import kotlin.math.ln

class MediaUtils

/**
 * Ensure this class is only used as a utility.
 */
private constructor() {

    init {
        throw AssertionError()
    }

    companion object {

        fun doesMediaFileExist(file : File) : Boolean {
            return file.exists()
        }

        fun deleteMediaFileIfExist(file : File) : Boolean {
            if (doesMediaFileExist(file)) {
                return file.delete()
            }
            return false
        }

        fun getTitleAsFilename(title: String) : String {

            var filename = title
                    .replace('-', ' ')
                    .replace(':', ' ')
                    .trim()
                    .replace("\\s{2,}".toRegex(), " ")
                    .replace('\\', '.')
                    .replace('|', '.')
                    .replace('/', '.')
                    .replace(',', '.')
                    .replace(".|.", ".")
                    .replace(' ', '.')
            filename = Normalizer.normalize(filename, Normalizer.Form.NFKD)
            filename = filename
                    .replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")
                    .replace("\\.{2,}".toRegex(), ".")

            return filename
        }

        fun getUniqueFilename(file : File) : String  {
            return if (!doesMediaFileExist(file)) file.name else internalGetUniqueFilename(file, 1)
        }

        private fun internalGetUniqueFilename(originalFile : File, index : Int) : String  {
            val extension = originalFile.name.substring(originalFile.name.lastIndexOf("."))
            val filename = originalFile.name.substring(0, originalFile.name.lastIndexOf("."))
            val fullFilename = "$filename($index)$extension"
            val file = File(originalFile.parentFile, fullFilename)
            return if (!doesMediaFileExist(file)) file.name else internalGetUniqueFilename(originalFile, index + 1)
        }

        fun getUniqueFilenameAndLock(dirPath : String, filename : String) : String  {
            return getUniqueFilenameAndLock(File(dirPath, filename))
        }

        private fun getUniqueFilenameAndLock(file : File) : String  {
            return if (!doesMediaFileExist(file) && FilenameLockerAdapter.instance.put(file.name))
                file.name
            else
                internalGetUniqueFilenameAndLock(file, 1)
        }

        private fun internalGetUniqueFilenameAndLock(originalFile : File, index : Int) : String  {
            val lastIndexOf = originalFile.name.lastIndexOf(".")
            if (lastIndexOf < 0) return originalFile.name;
            val extension = originalFile.name.substring(originalFile.name.lastIndexOf("."))
            val filename = originalFile.name.substring(0, originalFile.name.lastIndexOf("."))
            val fullFilename = "$filename($index)$extension"
            val file = File(originalFile.parentFile, fullFilename)
            return if (!doesMediaFileExist(file) && FilenameLockerAdapter.instance.put(fullFilename))
                file.name
            else
                internalGetUniqueFilenameAndLock(originalFile, index + 1)
        }

        fun humanReadableByteCount(bytes: Long?, si: Boolean): String {
            return humanReadableByteCount(bytes ?: 0, si)
        }

        fun humanReadableByteCount(bytes: Long, si: Boolean): String {
            val unit = if (si) 1000 else 1024
            if (bytes < unit) return "$bytes B"
            val exp = (ln(bytes.toDouble()) / ln(unit.toDouble())).toInt()
            val pre = (if (si) "kMGTPE" else "KMGTPE")[exp - 1] + if (si) "" else "i"
            return String.format("%.1f %sB", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
        }

        fun humanReadableTime(time: Long): String {
            return when {
                time < 1000 -> // less than one second
                    "0s"
                time / 1000 < 60 -> // less than 60 second
                    (time / 1000).toString() + "s"
                time / 1000 / 60 < 60 -> // less than 60 minutes
                    (time / 1000 / 60).toString() + "min"
                time / 1000 / 60 / 60 < 24 -> // less than 24h
                    (time / 1000 / 60 / 60).toString() + "h"
                else -> // in days
                    (time / 1000 / 60 / 60 / 24).toString() + "days"
            }
        }

        fun calculateRemainingDownloadTime(oldTimestamp: Long,
                                           timestamp: Long,
                                           oldProgress: Long,
                                           progress: Long,
                                           total: Long): Long {
            return if (timestamp > oldTimestamp && progress > oldProgress) {
                (total - progress) * (timestamp - oldTimestamp) / (progress - oldProgress)
            } else {
                0
            }
        }

        fun calculateDownloadingSpeed(oldTimestamp: Long,
                                      timestamp: Long,
                                      oldProgress: Long,
                                      progress: Long): Float {
            return if (timestamp > oldTimestamp && progress > oldProgress) {
                (progress - oldProgress).toFloat() * 1000f / (timestamp - oldTimestamp).toFloat()
            } else {
                0f
            }
        }
    }
}