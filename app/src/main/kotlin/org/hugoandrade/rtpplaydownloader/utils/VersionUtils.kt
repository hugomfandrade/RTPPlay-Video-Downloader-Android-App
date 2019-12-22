package org.hugoandrade.rtpplaydownloader.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.microsoft.windowsazure.mobileservices.MobileServiceClient
import com.microsoft.windowsazure.mobileservices.table.MobileServiceJsonTable
import org.hugoandrade.rtpplaydownloader.network.DownloadableItemAction
import org.jsoup.Jsoup
import java.io.IOException
import java.net.MalformedURLException

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

        fun checkIfAppNeedsUpdateFromGooglePlay(context : Context) : ListenableFutureImpl<String> {

            val currentUpdateFuture : ListenableFutureImpl<String> = ListenableFutureImpl()

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

        private const val appUrl: String =  "https://hugoandrade-apps-container.azurewebsites.net"
        private const val appTableName: String =  "RTPPlayAppVersion"
        private const val appTableCurrentVersion: String =  "CurrentVersion"

        fun checkIfAppNeedsUpdateFromAzure(context : Context): ListenableFutureImpl<String> {

            val currentUpdateFuture : ListenableFutureImpl<String> = ListenableFutureImpl()

            if (!NetworkUtils.isNetworkAvailable(context)) {
                currentUpdateFuture.failed("no network")
                return currentUpdateFuture
            }

            try {
                val mClient = MobileServiceClient(
                        appUrl,
                        null,
                        context)

                val future = MobileServiceJsonTable(appTableName, mClient)

                        .execute()
                Futures.addCallback(future, object : FutureCallback<JsonElement> {
                    override fun onSuccess(jsonObject: JsonElement?) {
                        try {
                            if (jsonObject == null) {
                                currentUpdateFuture.failed("failed to read from cloud: null")
                                return
                            }

                            if (!jsonObject.isJsonArray && jsonObject.asJsonArray.size() == 0) {
                                currentUpdateFuture.failed("failed to read from cloud: zero or is not array")
                                return
                            }

                            val jsonElement: JsonElement = jsonObject.asJsonArray.get(0)

                            currentUpdateFuture.success(jsonElement.asJsonObject.get(appTableCurrentVersion).asString)
                        }
                        catch (e: Exception) {
                            currentUpdateFuture.failed("failed to read from cloud: " + e.message)
                        }
                    }

                    override fun onFailure(t: Throwable) {
                        currentUpdateFuture.failed("failed to read from cloud (failed): " + t.message)
                    }
                })
            } catch (e: MalformedURLException) {
                e.printStackTrace()
                currentUpdateFuture.failed("Url is malformed")
            } catch (e: Exception) {
                e.printStackTrace()
                currentUpdateFuture.failed(e.message.toString())
            }

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

        private const val appTableHistoryName: String =  "RTPPlayAppDownloadHistory"
        private const val appTableHistoryOriginalUrl: String =  "OriginalUrl"
        private const val appTableHistoryUrl: String =  "Url"
        private const val appTableHistoryUrlTaskID: String =  "UrlTaskID"

        fun uploadHistory(context : Context, downloadableItem: DownloadableItemAction): ListenableFutureImpl<String> {

            val uploadHistoryFuture : ListenableFutureImpl<String> = ListenableFutureImpl()

            if (!NetworkUtils.isNetworkAvailable(context)) {
                uploadHistoryFuture.failed("no network")
                return uploadHistoryFuture
            }

            val insertObject = JsonObject()
            insertObject.addProperty(appTableHistoryOriginalUrl, downloadableItem.item.url)
            insertObject.addProperty(appTableHistoryUrl, downloadableItem.item.mediaUrl)

            try {
                val mClient = MobileServiceClient(
                        appUrl,
                        null,
                        context)

                val future = MobileServiceJsonTable(appTableHistoryName, mClient)
                        .insert(insertObject)
                Futures.addCallback(future, object : FutureCallback<JsonObject> {
                    override fun onSuccess(jsonObject: JsonObject?) {
                        uploadHistoryFuture.success("history successfully uploaded history")
                    }

                    override fun onFailure(t: Throwable) {
                        uploadHistoryFuture.failed("failed to uploaded history: " + t.message)
                    }
                })
            } catch (e: MalformedURLException) {
                e.printStackTrace()
                uploadHistoryFuture.failed("Url is malformed")
            } catch (e: Exception) {
                e.printStackTrace()
                uploadHistoryFuture.failed(e.message.toString())
            }

            return uploadHistoryFuture
        }
    }
}