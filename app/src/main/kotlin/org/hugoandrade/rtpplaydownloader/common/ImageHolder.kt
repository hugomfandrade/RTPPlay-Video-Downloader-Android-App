package org.hugoandrade.rtpplaydownloader.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.util.Log
import android.widget.ImageView
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.net.MalformedURLException

class ImageHolder(private val mImageView: ImageView?,
                  private val mDefaultImageResource: Int,
                  private val mInSampleSize: Int = 0) {

    constructor(imageView: ImageView?,
                filePath: String?,
                defaultImageResource: Int,
                inSampleSize: Int): this(imageView, defaultImageResource, inSampleSize) {

        displayImage(filePath)
    }

    constructor(imageView: ImageView?,
                fileUri: String?,
                defaultImageResource: Int,
                inSampleSize: Int,
                filesDir: File?): this(imageView, defaultImageResource, inSampleSize) {

        val f = getImageUriIfExists(fileUri, filesDir)

        if (f == null) {
            mImageView?.setImageResource(mDefaultImageResource)
            if (filesDir == null) {
                return
            }
            DownloadImageAsyncTask(filesDir)
                    .setOnFinishedListener(object : DownloadImageAsyncTask.OnFinishedListener {
                        override fun onFinished(filePath: String?) {
                            displayImage(filePath)
                        }
                    })
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, fileUri)
        } else {
            displayImage(f.absolutePath)
        }
    }

    private fun displayImage(filePath: String?) {
        if (filePath == null) {
            mImageView?.setImageResource(mDefaultImageResource)
            return
        }
        val cacheBitmap : Bitmap? = ImageCacheAdapter.instance.get(filePath, mInSampleSize)

        if (cacheBitmap != null) {
            mImageView?.setImageBitmap(cacheBitmap)
            return
        }

        mImageView?.setImageResource(mDefaultImageResource)

        DisplayImageAsyncTask(mInSampleSize)
                .setOnFinishedListener(object : DisplayImageAsyncTask.OnFinishedListener {
                    override fun onFinished(result: Bitmap?) {

                        if (result != null) {
                            mImageView?.setImageBitmap(result)
                            return
                        }

                        if (mDefaultImageResource != -1) {
                            mImageView?.setImageResource(mDefaultImageResource)
                        }
                    }
                })
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, filePath)
    }

    private fun getImageUriIfExists(imageUri: String?, filesDir: File?): File? {
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

    class DisplayImageAsyncTask internal constructor(private val mInSampleSize: Int) : AsyncTask<String, Void, Bitmap>() {

        private val TAG = javaClass.simpleName
        private var mListener: OnFinishedListener? = null

        override fun doInBackground(vararg filePaths: String): Bitmap? {

            val filePath = filePaths[0] ?: return null

            val f = File(filePath)

            val options = BitmapFactory.Options()
            options.inSampleSize = mInSampleSize
            // down sizing image as it throws OutOfMemory Exception for larger images

            if (f.exists()) {
                val bitmap = BitmapFactory.decodeFile(f.absolutePath, options)
                if (bitmap != null) {
                    ImageCacheAdapter.instance.put(filePath, mInSampleSize, bitmap)
                    return bitmap
                }
            }
            return null
        }

        override fun onPostExecute(result: Bitmap?) {
            mListener?.onFinished(result)
        }

        internal fun setOnFinishedListener(listener: OnFinishedListener): DisplayImageAsyncTask {
            mListener = listener
            return this
        }

        interface OnFinishedListener {
            fun onFinished(result: Bitmap?)
        }
    }

    private class DownloadImageAsyncTask internal constructor(private val mFilesDir: File) : AsyncTask<String, Void, String>() {

        private val TAG = javaClass.simpleName
        private var mListener: OnFinishedListener? = null

        override fun doInBackground(vararg filePaths: String): String? {

            val imageURL = filePaths[0]

            try {
                // Download Image from URL
                val input = java.net.URL(imageURL).openStream()
                // Decode Bitmap
                val bitmap = BitmapFactory.decodeStream(input)

                val a = imageURL.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val filename = a[a.size - 1]

                val pictureFile = File(mFilesDir, filename)
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

        override fun onPostExecute(path: String?) {
            mListener?.onFinished(path)
        }

        internal fun setOnFinishedListener(listener: OnFinishedListener): DownloadImageAsyncTask {
            mListener = listener
            return this
        }

        interface OnFinishedListener {
            fun onFinished(filePath: String?)
        }
    }

    class Builder(imageView: ImageView) {

        private val P: ImageHolderParams = ImageHolderParams(imageView)

        fun setFileUrl(fileUrl: String): Builder {
            P.fileUrl = fileUrl
            P.mode = ImageHolderParams.FILE_URL
            return this
        }

        fun setFilePath(filePath: String): Builder {
            P.filePath = filePath
            P.mode = ImageHolderParams.FILE_PATH
            return this
        }

        fun setDefaultImageResource(defaultImageResource: Int): Builder {
            P.defaultImageResource = defaultImageResource
            return this
        }

        fun setInSampleSize(inSampleSize: Int): Builder {
            P.inSampleSize = inSampleSize
            return this
        }

        fun setFileDir(filesDir: File): Builder {
            P.filesDir = filesDir
            return this
        }

        fun execute() {

            when (P.mode) {
                ImageHolderParams.FILE_URL -> {
                    if (P.filesDir == null)
                        P.filesDir = P.imageView.context.getExternalFilesDir(null)
                    ImageHolder(P.imageView,
                            P.fileUrl,
                            P.defaultImageResource,
                            P.inSampleSize,
                            P.filesDir)
                }
                ImageHolderParams.FILE_PATH -> ImageHolder(P.imageView,
                        P.filePath,
                        P.defaultImageResource,
                        P.inSampleSize)
            }
        }

        companion object {

            fun instance(imageView: ImageView): Builder {
                return Builder(imageView)
            }
        }
    }

    private class ImageHolderParams internal constructor(val imageView: ImageView) {
        var filesDir: File? = null
        var fileUrl: String? = null
        var filePath: String? = null
        var defaultImageResource = -1
        var inSampleSize = 8

        var mode: Int = 0

        companion object {

            internal const val FILE_URL = 1
            internal const val FILE_PATH = 2
        }
    }
}