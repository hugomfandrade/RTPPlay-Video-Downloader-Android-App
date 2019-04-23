package org.hugoandrade.rtpplaydownloader.common

import android.content.Context

interface ContextView {

    /**
     * Get the Activity Context.
     */
    fun getActivityContext(): Context

    /**
     * Get the Application Context.
     */
    fun getApplicationContext(): Context
}