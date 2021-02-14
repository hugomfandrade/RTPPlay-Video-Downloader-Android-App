package org.hugoandrade.rtpplaydownloader.network.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import org.hugoandrade.rtpplaydownloader.R
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL

class NetworkUtils
/**
 * Ensure this class is only used as a utility.
 */
private constructor() {

    init {
        throw AssertionError()
    }


    companion object {

        fun getDoc(url: String): Document? {
            return getDoc(url, 10000);
        }

        fun getDoc(url: String, millis: Int): Document? {
            return try {
                Jsoup.connect(url).timeout(millis).get()
            } catch (e : java.lang.Exception) {
                e.printStackTrace()
                null
            }
        }

        fun isValidURL(urlText: String): Boolean {
            return try {
                val url = URL(urlText)
                "http" == url.protocol || "https" == url.protocol
            } catch (e: Exception) {
                false
            }
        }

        fun register(context: Context, iNetworkBroadcastReceiver: INetworkBroadcastReceiver): BroadcastReceiver {

            val broadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    iNetworkBroadcastReceiver.setNetworkAvailable(isNetworkAvailable(context))
                }
            }

            context.registerReceiver(broadcastReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

            return broadcastReceiver
        }

        fun unregister(context: Context, broadcastReceiver: BroadcastReceiver) {
            context.unregisterReceiver(broadcastReceiver)
        }

        fun isNetworkAvailable(context: Context?): Boolean {
            try {
                if (context == null) return false

                val connMgr = context
                        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val networks = connMgr.allNetworks


                    var wifiAvailability = false
                    var mobileAvailability = false

                    for (network in networks) {
                        val networkInfo = connMgr.getNetworkInfo(network) ?: continue

                        if (networkInfo.type == ConnectivityManager.TYPE_WIFI && networkInfo.isAvailable) {
                            wifiAvailability = true
                        } else if (networkInfo.type == ConnectivityManager.TYPE_MOBILE && networkInfo.isAvailable) {
                            mobileAvailability = true
                        }
                    }

                    return wifiAvailability || mobileAvailability
                } else {

                    val activeNetwork = connMgr.activeNetworkInfo
                    var wifiAvailability = false
                    var mobileAvailability = false
                    if (activeNetwork != null) { // connected to the internet
                        if (activeNetwork.type == ConnectivityManager.TYPE_WIFI) {
                            wifiAvailability = true
                        } else if (activeNetwork.type == ConnectivityManager.TYPE_MOBILE) {
                            mobileAvailability = true
                        }
                    }

                    return wifiAvailability || mobileAvailability
                }
            } catch (e: NullPointerException) {
                return false
            }

        }

        fun isNetworkUnavailableError(context: Context?, message: String?): Boolean {

            return (message != null
                    && context != null
                    && message == context.getString(R.string.no_network_connection))

        }
    }

    interface INetworkBroadcastReceiver {

        fun setNetworkAvailable(isNetworkAvailable: Boolean)
    }
}