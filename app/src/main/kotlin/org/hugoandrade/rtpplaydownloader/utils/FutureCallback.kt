package org.hugoandrade.rtpplaydownloader.utils

interface FutureCallback<T> {
    fun onFailed(errorMessage: String)
    fun onSuccess(result: T?)
}