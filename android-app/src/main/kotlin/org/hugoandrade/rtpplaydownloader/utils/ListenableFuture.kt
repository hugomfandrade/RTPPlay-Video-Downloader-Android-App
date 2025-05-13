package org.hugoandrade.rtpplaydownloader.utils

import java.util.concurrent.ExecutionException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class ListenableFuture<T> {

    private var isFinished: Boolean = false
    private var isSuccess: Boolean = false
    private var errorMessage: String? = null
    private var result: T? = null

    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    private var callback: Callback<T>? = null

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
    fun get(): T? {
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
    fun addCallback(callback: Callback<T>) {
        this.callback = callback

        fireCallback()
    }

    private fun fireCallback() {
        if (isFinished) {
            if (isSuccess) {
                result?.let { callback?.onSuccess(it) }
            } else {
                errorMessage?.let { callback?.onFailed(it) }
            }
        }
    }

    interface Callback<T> {
        fun onFailed(errorMessage: String)
        fun onSuccess(result: T)
    }
}