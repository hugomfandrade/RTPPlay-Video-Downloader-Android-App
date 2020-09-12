package org.hugoandrade.rtpplaydownloader.network.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import org.hugoandrade.rtpplaydownloader.R
import org.hugoandrade.rtpplaydownloader.network.DownloadableItem
import org.hugoandrade.rtpplaydownloader.utils.ViewUtils
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

        const val sharedPreferencesName = "org.hugoandrade.rtpplaydownloader"
        const val directoryKey = "org.hugoandrade.rtpplaydownloader.directoryKey"

        // private val defaultDir : File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        private fun getDefaultDir() : File {
            return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        }

        fun getDownloadsDirectory(context: Context) : Uri {

            try {
                return Uri.parse(context.getSharedPreferences(sharedPreferencesName, Context.MODE_PRIVATE)
                        .getString(directoryKey, getDefaultDir().toString()))
            }
            catch (e : Exception) {
                return Uri.fromFile(getDefaultDir())
            }
        }

        fun putDownloadsDirectory(context: Context, uri: Uri) {
            putDownloadsDirectory(context, uri.toString())
        }

        fun putDownloadsDirectory(context: Context, uri: String) {

            context.getSharedPreferences(sharedPreferencesName, Context.MODE_PRIVATE)
                    .edit()
                    .putString(directoryKey, uri)
                    .apply()
        }

        fun doesMediaFileExist(file : File) : Boolean {
            return file.exists()
        }

        fun doesMediaFileExist(item : DownloadableItem) : Boolean {
            val filepath = item.filepath

            if (filepath != null) {

                // val storagePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString()
                // val file = File(storagePath, item.mediaFileName)
                // val storagePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString()
                val file = File(filepath)
                return file.exists()
            }
            return false
        }

        fun deleteMediaFileIfExist(item: DownloadableItem) : Boolean {

            val filepath = item.filepath

            if (filepath != null) {

                val file = File(filepath)
                if (doesMediaFileExist(file)) {
                    return file.delete()
                }
                return false
            }
            return false
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

        fun getUniqueFilename(filename : String) : String  {
            return getUniqueFilename(File(getDefaultDir().toString(), filename))
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

        fun getUniqueFilenameAndLock(filename : String) : String  {
            val defaultDirPath = getDefaultDir().toString()
            return getUniqueFilenameAndLock(defaultDirPath, filename)
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

        fun showInFolderIntent(context: Context, item: DownloadableItem) {

            try {
                val dir = Uri.parse(File(item.filepath).parentFile.absolutePath + File.separator)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    intent.addCategory(Intent.CATEGORY_DEFAULT)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, dir)
                    }

                    intent.putExtra("android.content.extra.SHOW_ADVANCED", true)
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_folder)))
                } else {
                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                    intent.setDataAndType(dir, "*/*")
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_folder)))
                }

            } catch (e: Exception) { }
        }

        fun openUrl(context: Context, item: DownloadableItem) {

            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
            } catch (e: Exception) { }
        }

        fun play(context: Context, item: DownloadableItem) {

            try {
                val filepath = item.filepath
                if (doesMediaFileExist(item)) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(filepath))
                            .setDataAndType(Uri.parse(filepath), "video/mp4"))
                } else {
                    ViewUtils.showToast(context, context.getString(R.string.file_not_found))
                }
            } catch (ignored: Exception) {
            }
        }
    }
}