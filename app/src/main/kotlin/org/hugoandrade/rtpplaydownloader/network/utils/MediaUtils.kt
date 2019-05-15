package org.hugoandrade.rtpplaydownloader.network.utils

import android.os.Environment
import org.hugoandrade.rtpplaydownloader.network.DownloadableItem
import java.io.File
import java.text.Normalizer

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

        fun doesMediaFileExist(item : DownloadableItem) : Boolean {
            val filepath = item.filepath

            if (filepath != null) {

                // val storagePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString()
                // val file = File(storagePath, item.filename)
                // val storagePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString()
                val file = File(filepath)
                return file.exists()
            }
            return false
        }

        fun getTitleAsFilename(title: String) : String {

            var filename = title
                    .replace('-', ' ')
                    .replace(':', ' ')
                    .replace("\\s{2,}".toRegex(), " ")
                    .trim()
                    .replace('\\', '.')
                    .replace('|', '.')
                    .replace('/', '.')
                    .replace(".|.", ".")
                    .replace(' ', '.')
            filename = Normalizer.normalize(filename, Normalizer.Form.NFKD)
            filename = filename
                    .replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")

            return filename
        }

        fun getUniqueFilename(filename : String) : String  {
            val storagePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString()
            return getUniqueFilename(File(storagePath, filename))
        }

        fun getUniqueFilename(file : File) : String  {
            return if (!doesMediaFileExist(file)) file.name else internalGetUniqueFilename(file, 1)
        }

        private fun internalGetUniqueFilename(originalFile : File, index : Int) : String  {
            val extension = originalFile.name.substring(originalFile.name.lastIndexOf("."))
            val filename = originalFile.name.substring(0, originalFile.name.lastIndexOf("."))
            val fullFilename = "$filename($index)$extension"
            val file = File(originalFile.parentFile, fullFilename)
            return if (!doesMediaFileExist(file)) file.name else internalGetUniqueFilename(file, index + 1)
        }
    }
}