package org.hugoandrade.rtpplaydownloader.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.ImageView
import org.hugoandrade.rtpplaydownloader.DevConstants
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

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
                var inputStream = URL(imageURL).openStream()
                var bitmap = BitmapFactory.decodeStream(inputStream)

                if (bitmap == null) {
                    inputStream.close()
                    inputStream = URL(imageURL.replace("http", "https")).openStream()
                    bitmap = BitmapFactory.decodeStream(inputStream)
                }

                val filename = getFilename(imageURL)

                val pictureFile = File(dir, filename)
                try {
                    val fos = FileOutputStream(pictureFile)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    fos.close()
                    inputStream.close()
                } catch (e: FileNotFoundException) {
                    Log.e(TAG, "File not found: " + e.message)
                    pictureFile.delete()
                    inputStream.close()
                    return null
                } catch (e: IOException) {
                    Log.e(TAG, "Error accessing file: " + e.message)
                    pictureFile.delete()
                    inputStream.close()
                    return null
                }catch (e: Exception) {
                    Log.e(TAG, "Error: " + e.message)
                    pictureFile.delete()
                    inputStream.close()
                    return null
                }

                return pictureFile.absolutePath

            } catch (e: MalformedURLException) {
                Log.w(TAG, "MalformedURLException: $imageURL")
            } catch (e: IOException) {
                Log.e(TAG, "IOException: $imageURL")
            } catch (e: Exception) {
                Log.e(TAG, "Exception: $imageURL")
                e.printStackTrace()
            }

            return null
        }

        private fun getFilename(imageURL: String): String {
            return imageURL
                    .replace("/", "_")
                    .replace(".", "_")
        }

        fun getImageUriIfExists(filesDir: File?, imageUri: String?): File? {
            if (imageUri == null)
                return null

            val filename = getFilename(imageUri)

            if (filesDir == null)
                return null
            try {
                for (f in filesDir.listFiles()) {
                    if (f.name == filename)
                        return f
                }
                return null
            } catch (e: NullPointerException) {
                e.printStackTrace()
                return null
            }
        }

        private val bitmapCacheMap: ConcurrentHashMap<String, WeakReference<Bitmap>> = ConcurrentHashMap()
        private val imageViewCacheMap: ConcurrentHashMap<Int, String> = ConcurrentHashMap()
        private val imageLoaderExecutors = Executors.newFixedThreadPool(DevConstants.nImageLoadingThreads)

        fun displayImage(dir: File?, url: String?, imageView: ImageView, defaultResID : Int?) {

            if (url != null) {
                imageViewCacheMap[imageView.hashCode()] = url

                val cacheBitmap = bitmapCacheMap[url]?.get()

                if (cacheBitmap != null) {
                    imageView.setImageBitmap(cacheBitmap)
                    return
                }
            }

            if (defaultResID != null) {
                imageView.setImageResource(defaultResID)
            }

            if (dir != null && url != null) {

                imageLoaderExecutors.submit {

                    val imageViewRef: WeakReference<ImageView> = WeakReference(imageView)
                    val imageHolder = ImageHolder(dir)

                    // load from cache, if it exists
                    var bitmap = imageHolder.loadFromCache(url)

                    if (bitmap == null) {
                        bitmap = imageHolder.load(url)
                    }

                    if (bitmap != null) {
                        bitmapCacheMap[url] = WeakReference(bitmap)

                        displayImage(imageViewRef, url, bitmap)
                    }
                }
            }
        }

        private fun displayImage(ref: WeakReference<ImageView>, refUrl: String, bitmap: Bitmap) {

            val refImageView : ImageView = ref.get()?: return

            // get current search item
            val mapUrl: String? = imageViewCacheMap[refImageView.hashCode()]?: return

            if (refUrl == mapUrl) {
                refImageView.post {

                    val refImageView: ImageView = ref.get()?: return@post

                    // get current search item
                    val mapUrl = imageViewCacheMap[refImageView.hashCode()]?: return@post

                    if (refUrl == mapUrl) {
                        refImageView.setImageBitmap(bitmap)
                    }
                }
            }
        }
    }
}