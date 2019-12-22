package org.hugoandrade.rtpplaydownloader.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.ImageView
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import org.hugoandrade.rtpplaydownloader.DevConstants

class ImageHolder(private val mDir: File) {

    class Builder {

        private var dir: File? = null
        private var defaultResID: Int? = null
        private var thumbnailUrl: String? = null

        fun withDefault(resID: Int): Builder {
            this.defaultResID = resID
            return this
        }

        fun toDir(dir: File?): Builder {
            this.dir = dir
            return this
        }

        fun download(thumbnailUrl: String?): Builder {
            this.thumbnailUrl = thumbnailUrl
            return this
        }

        fun displayIn(imageView: ImageView) {
            displayImage(dir, thumbnailUrl, imageView, defaultResID)
        }
    }

    fun loadFromCache(imageURL: String): Bitmap? {

        val cacheFile = getImageUriIfExists(mDir, imageURL) ?: return null

        return displayImage(cacheFile.absolutePath) ?: return null
    }

    fun load(imageURL: String): Bitmap? {

        val filePath: String? = downloadImage(mDir, imageURL)

        if (filePath == null) {
            if (DevConstants.showLog) Log.e(TAG, "failed to download file to cache dir")
            return null
        }

        val bitmap: Bitmap? = displayImage(filePath)

        if (bitmap == null) {
            if (DevConstants.showLog) Log.e(TAG, "failed to get bitmap from cache dir")
            return null
        }

        return bitmap
    }

    fun download(imageURL: String): String? {

        val filePath: String? = downloadImage(mDir, imageURL)

        if (filePath == null) {
            if (DevConstants.showLog) Log.e(TAG, "failed to download file to cache dir")
            return null
        }

        return filePath
    }

    companion object {

        private const val TAG : String = "ImageHolder"

        fun displayImage(filePath: String): Bitmap? {

            val f = File(filePath)

            val options = BitmapFactory.Options()
            options.inSampleSize = 1
            // down sizing image as it throws OutOfMemory Exception for larger images

            if (f.exists()) {
                val bitmap = BitmapFactory.decodeFile(f.absolutePath, options)
                if (bitmap != null) {
                    return bitmap
                }
            }
            return null
        }

        fun downloadImage(dir: File, imageURL: String): String? {

            val cacheFile = getImageUriIfExists(dir, imageURL)

            if (cacheFile != null) {
                return cacheFile.absolutePath
            }

            try {
                // Download Image from URL
                val input = URL(imageURL).openStream()
                // Decode Bitmap
                val bitmap = BitmapFactory.decodeStream(input)

                val a = imageURL.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val filename = a[a.size - 1]

                val pictureFile = File(dir, filename)
                try {
                    val fos = FileOutputStream(pictureFile)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    fos.close()
                } catch (e: FileNotFoundException) {
                    Log.e(TAG, "File not found: " + e.message)
                    return null
                } catch (e: IOException) {
                    Log.e(TAG, "Error accessing file: " + e.message)
                    return null
                }

                return pictureFile.absolutePath

            } catch (e: MalformedURLException) {
                Log.w(TAG, "MalformedURLException: $imageURL")
            } catch (e: IOException) {
                Log.e(TAG, "IOException: $imageURL")
            }

            return null
        }

        fun getImageUriIfExists(filesDir: File?, imageUri: String?): File? {
            if (imageUri == null)
                return null

            val a = imageUri.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val filename = a[a.size - 1]

            if (filesDir == null)
                return null
            try {
                for (f in filesDir.listFiles()) {
                    if (f.name == filename)
                        return f
                }
                return null
            } catch (e: NullPointerException) {
                return null
            }
        }

        private val imageViewCacheMap: ConcurrentHashMap<Int, String> = ConcurrentHashMap()
        private val imageLoaderExecutors = Executors.newFixedThreadPool(DevConstants.nImageLoadingThreads)

        fun displayImage(dir: File?, url: String?, imageView: ImageView, defaultResID : Int?) {
            if (dir != null && url != null) {

                val imageHolder = ImageHolder(dir)

                // load from cache, if it exists
                val bitmap = imageHolder.loadFromCache(url)

                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    return
                }
                else {
                    val imageViewRef: WeakReference<ImageView> = WeakReference(imageView)

                    imageViewCacheMap[imageView.hashCode()] = url

                    imageLoaderExecutors.submit {
                        // get bitmap
                        displayImage(imageViewRef, url, imageHolder.load(url))
                    }
                }
            }
            if (defaultResID != null) {
                imageView.setImageResource(defaultResID)
            }
        }

        private fun displayImage(ref: WeakReference<ImageView>, url: String, bitmap: Bitmap?) {


            if (bitmap != null) {

                val refImageView : ImageView? = ref.get()

                if (refImageView != null) {

                    // get current search item
                    val mapUrl: String? = imageViewCacheMap[refImageView.hashCode()]

                    if (mapUrl != null && url == mapUrl) {
                        refImageView.post {
                            refImageView.setImageBitmap(bitmap)
                        }
                    }
                }
            }
        }
    }
}