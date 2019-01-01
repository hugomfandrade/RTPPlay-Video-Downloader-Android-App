package org.hugoandrade.rtpplaydownloader.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;

import org.hugoandrade.rtpplaydownloader.R;

public final class NetworkUtils {

    /**
     * Ensure this class is only used as a utility.
     */
    private NetworkUtils() {
        throw new AssertionError();
    }

    public static BroadcastReceiver register(Context context, final INetworkBroadcastReceiver iNetworkBroadcastReceiver) {

        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                iNetworkBroadcastReceiver.setNetworkAvailable(isNetworkAvailable(context));
            }
        };

        context.registerReceiver(broadcastReceiver,  new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        return broadcastReceiver;
    }

    public static void unregister(Context context, BroadcastReceiver broadcastReceiver) {
        context.unregisterReceiver(broadcastReceiver);
    }

    public interface INetworkBroadcastReceiver {

        void setNetworkAvailable(boolean isNetworkAvailable);
    }

    public static boolean isNetworkAvailable(Context context) {
        try {
            if (context == null) return false;

            final ConnectivityManager connMgr = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);

            if (connMgr == null) {
                return false;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                final Network[] networks = connMgr.getAllNetworks();


                boolean wifiAvailability = false;
                boolean mobileAvailability = false;

                for (Network network : networks) {
                    NetworkInfo networkInfo = connMgr.getNetworkInfo(network);

                    if (networkInfo == null)
                        continue;

                    if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI &&
                            networkInfo.isAvailable()) {
                        wifiAvailability = true;
                    } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE &&
                            networkInfo.isAvailable()) {
                        mobileAvailability = true;
                    }
                }

                return wifiAvailability || mobileAvailability;
            } else {

                NetworkInfo activeNetwork = connMgr.getActiveNetworkInfo();
                boolean wifiAvailability = false;
                boolean mobileAvailability = false;
                if (activeNetwork != null) { // connected to the internet
                    if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                        wifiAvailability = true;
                    } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                        mobileAvailability = true;
                    }
                }

                return wifiAvailability || mobileAvailability;
            }
        } catch (NullPointerException e) {
            return false;
        }
    }

    public static boolean isNetworkUnavailableError(Context context, String message) {

        return message != null
                && context != null
                && message.equals(context.getString(R.string.no_network_connection));

    }
}
