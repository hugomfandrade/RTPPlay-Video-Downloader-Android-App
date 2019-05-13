package org.hugoandrade.rtpplaydownloader.network.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import org.hugoandrade.rtpplaydownloader.R
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

        fun getTitleAsFilename(title: String): String {

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