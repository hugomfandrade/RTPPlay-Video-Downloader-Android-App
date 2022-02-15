package org.hugoandrade.rtpplaydownloader.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import org.hugoandrade.rtpplaydownloader.network.DownloadableItemAction
import org.jsoup.Jsoup
import java.io.IOException

class VersionUtils
/**
 * Ensure this class is only used as a utility.
 */
private constructor() {

    init {
        throw AssertionError()
    }

    companion object {

        /**
         * String used in logging output.
         */
        private val TAG = VersionUtils::class.java.simpleName

        fun checkIfAppNeedsUpdateFromGooglePlay(context : Context) : ListenableFuture<String> {

            val currentUpdateFuture : ListenableFuture<String> = ListenableFuture()

            object : Thread("Look-For-Version-Update-Thread") {

                override fun run() {

                    try {
                        currentUpdateFuture.success(Jsoup.connect("https://play.google.com/store/apps/details?id=" + context.packageName + "&hl=en")
                                .timeout(30000)
                                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                                .referrer("http://www.google.com")
                                .get()
                                .select("div[itemprop=softwareVersion]")
                                .first()
                                .ownText())

                    } catch (e: IOException) {
                        e.printStackTrace()
                        currentUpdateFuture.failed(e.message.toString())
                    }
                }

            }.start()

            return currentUpdateFuture
        }

        fun checkIfAppNeedsUpdateFromAzure(context : Context): ListenableFuture<String> {

            val currentUpdateFuture : ListenableFuture<String> = ListenableFuture()
            currentUpdateFuture.failed("not implemented")
            return currentUpdateFuture
        }

        fun getCurrentVersion(context : Context): String? {

            val packageManager = context.packageManager
            val packageInfo: PackageInfo
            try {
                packageInfo = packageManager.getPackageInfo(context.packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                return null
            }

            return packageInfo.versionName
        }

        fun uploadHistory(context : Context, downloadableItem: DownloadableItemAction): ListenableFuture<String> {

            // TODO
            val uploadHistoryFuture : ListenableFuture<String> = ListenableFuture()
            uploadHistoryFuture.failed("not implemented")
            return uploadHistoryFuture
        }
    }
}