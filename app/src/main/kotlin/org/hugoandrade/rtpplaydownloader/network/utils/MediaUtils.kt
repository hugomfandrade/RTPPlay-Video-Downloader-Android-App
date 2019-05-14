package org.hugoandrade.rtpplaydownloader.network.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import android.os.Environment
import org.hugoandrade.rtpplaydownloader.R
import org.hugoandrade.rtpplaydownloader.network.DownloadableItem
import java.io.File
import java.net.URL
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

    }
}