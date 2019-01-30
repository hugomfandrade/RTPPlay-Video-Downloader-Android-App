package org.hugoandrade.rtpplaydownloader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;

import org.hugoandrade.rtpplaydownloader.common.ContextView;
import org.hugoandrade.rtpplaydownloader.common.RetainedFragmentManager;
import org.hugoandrade.rtpplaydownloader.utils.NetworkUtils;
import org.hugoandrade.rtpplaydownloader.utils.ViewUtils;

public abstract class ActivityBase extends AppCompatActivity

        implements ContextView {

    /**
     * Debugging tag used by the Android logger.
     */
    protected String TAG = getClass().getSimpleName();

    /**
     * Used to retain the objects between runtime
     * configuration changes.
     */
    private final RetainedFragmentManager mRetainedFragmentManager
            = new RetainedFragmentManager(this.getFragmentManager(),
            TAG);

    /**
     * Network UI/UX
     */
    private BroadcastReceiver mNetworkBroadcastReceiver;

    private View tvNoNetworkConnection;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mRetainedFragmentManager.firstTimeIn()) {
            // mRetainedFragmentManager.put(opsType.getSimpleName(), mPresenterInstance);
        }
        initializeNetworkFooter();
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);

        initializeNetworkFooter();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);

        initializeNetworkFooter();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);

        initializeNetworkFooter();
    }

    public RetainedFragmentManager getRetainedFragmentManager() {
        return mRetainedFragmentManager;
    }

    private void initializeNetworkFooter() {

        if (mNetworkBroadcastReceiver == null) {
            mNetworkBroadcastReceiver = NetworkUtils.register(this, iNetworkListener);
        }

        tvNoNetworkConnection = findViewById(R.id.tv_no_network_connection);

        ViewUtils.setHeightDp(this, tvNoNetworkConnection, NetworkUtils.isNetworkAvailable(this)? 0 : 20);
    }

    /**
     * Hook method called by Android when this Activity becomes
     * invisible.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mNetworkBroadcastReceiver != null) {
            NetworkUtils.unregister(this, mNetworkBroadcastReceiver);
            mNetworkBroadcastReceiver = null;
        }
    }

    private NetworkUtils.INetworkBroadcastReceiver iNetworkListener = new NetworkUtils.INetworkBroadcastReceiver() {

        @Override
        public void setNetworkAvailable(boolean isNetworkAvailable) {

            onNetworkStateChanged(isNetworkAvailable);

            ViewUtils.setHeightDpAnim(getApplicationContext(), tvNoNetworkConnection, isNetworkAvailable? 0 : 20);
        }
    };

    protected void onNetworkStateChanged(boolean isAvailable) {
    }

    /**
     * Return the Activity context.
     */
    @Override
    public Context getActivityContext() {
        return this;
    }

    /**
     * Return the Application context.
     */
    @Override
    public Context getApplicationContext() {
        return super.getApplicationContext();
    }
}
