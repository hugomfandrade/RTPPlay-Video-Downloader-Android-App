package org.hugoandrade.rtpplaydownloader.utils

import java.util.concurrent.ExecutionException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

open class ListenableFutureImpl<T> : ListenableFuture<T> {

    private var isFinished: Boolean = false
    private var isSuccess: Boolean = false
    private var errorMessage: String? = null
    private var result: T? = null

    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    private var onScanningCallback: FutureCallback<T>? = null

    @Synchronized
    internal fun failed(errorMessage: String): Boolean {
        if (isFinished) return false
        this.isFinished = true
        this.isSuccess = false
        this.errorMessage = errorMessage

        lock.withLock {
            condition.signal()
        }

        fireCallback()
        return true
    }

    @Synchronized
    internal fun success(result: T): Boolean {
        if (isFinished) return false
        this.isFinished = true
        this.isSuccess = true
        this.result = result

        lock.withLock {
            condition.signal()
        }

        fireCallback()
        return true
    }

    @Throws(InterruptedException::class, ExecutionException::class)
    override fun get(): T? {
        if (isFinished) {
            if (!isSuccess) throw ExecutionException(Throwable(errorMessage))
            return result
        }

        lock.withLock {
            condition.await()
        }

        if (!isFinished) throw ExecutionException(Throwable("task was not finished"))
        if (!isSuccess) throw ExecutionException(Throwable(errorMessage))

        return result
    }

    @Synchronized
    override fun addCallback(onScanningCallback: FutureCallback<T>) {
        this.onScanningCallback = onScanningCallback

        fireCallback()
    }

    private fun fireCallback() {
        if (isFinished) {
            if (isSuccess) {
                onScanningCallback?.onSuccess(result!!)
            } else {
                onScanningCallback?.onFailed(errorMessage!!)
            }
        }
    }
}