package org.hugoandrade.rtpplaydownloader.utils

import java.util.concurrent.ExecutionException

interface ListenableFuture<T> {

    @Throws(InterruptedException::class, ExecutionException::class)
    fun get(): T?

    fun addCallback(onScanningCallback: FutureCallback<T>)
}