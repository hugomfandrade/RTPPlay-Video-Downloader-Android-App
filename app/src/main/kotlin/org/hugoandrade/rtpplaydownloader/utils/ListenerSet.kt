package org.hugoandrade.rtpplaydownloader.utils

import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock

class ListenerSet<T> {

    private val listenerSet: HashSet<T> = HashSet()
    private val toAddListenerSet: HashSet<T> = HashSet()
    private val toRemoveListenerSet: HashSet<T> = HashSet()

    private val lock = ReentrantReadWriteLock()

    fun addListener(listener: T) {
        if (lock.writeLock().tryLock()) {
            try {
                select(listener).addTo(listenerSet)
            } finally {
                lock.writeLock().unlock()
            }
        } else {
            select(listener).addTo(toAddListenerSet)
        }
    }

    fun removeListener(listener: T) {
        if (lock.writeLock().tryLock()) {
            try {
                select(listener).removeFrom(listenerSet)
            } finally {
                lock.writeLock().unlock()
            }
        } else {
            select(listener).addTo(toRemoveListenerSet)
        }
    }

    fun get(): Set<T> {
        return listenerSet
    }

    fun lock() {
        lock.readLock().lock()
    }

    fun release() {
        lock.readLock().unlock()

        if (!lock.writeLock().tryLock()) return

        try {

            // populate while locked
            for (t in toRemoveListenerSet) {
                select(t).removeFrom(listenerSet)
            }
            toRemoveListenerSet.clear()

            for (t in toAddListenerSet) {
                select(t).addTo(listenerSet)
            }
            toAddListenerSet.clear()
        } finally {
            lock.writeLock().unlock()
        }
    }

    private fun select(listener: T): Edit {
        return Edit(listener)
    }

    private inner class Edit constructor(private val listener: T) {

        fun addTo(listenerSet: HashSet<T>) {
            listenerSet.add(listener)
        }

        fun removeFrom(listenerSet: HashSet<T>) {
            listenerSet.remove(listener)
        }

    }
}