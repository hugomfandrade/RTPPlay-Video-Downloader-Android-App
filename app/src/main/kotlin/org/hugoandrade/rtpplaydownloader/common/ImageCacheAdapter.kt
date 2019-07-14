package org.hugoandrade.rtpplaydownloader.common

import android.graphics.Bitmap
import android.text.TextUtils

class ImageCacheAdapter {

    companion object {
        val instance = ImageCacheAdapter()

        fun keyID(filePath: String, inSampleSize: Int) : String {
            return TextUtils.concat(filePath, inSampleSize.toString()).toString()
        }
    }

    private val sBitmaps = HashMap<String, Bitmap>()

    fun put(filePath: String, inSampleSize: Int, bitmap: Bitmap) {
        sBitmaps[keyID(filePath, inSampleSize)] = bitmap
    }

    fun get(filePath: String, inSampleSize: Int): Bitmap? {
        return sBitmaps[keyID(filePath, inSampleSize)]
    }

    fun contains(filePath: String, inSampleSize: Int): Boolean {
        return sBitmaps.containsKey(keyID(filePath, inSampleSize))
    }
}