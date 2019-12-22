package org.hugoandrade.rtpplaydownloader.utils

import java.util.*


class ListenerSet<T> {

    protected val listenerSet: HashSet<T> = HashSet()
    protected val toAddListenerSet: HashSet<T> = HashSet()
    protected val toRemoveListenerSet: HashSet<T> = HashSet()

    @get:Synchronized
    @Volatile
    var isLocked = false
        private set

    @Synchronized
    fun addListener(listener: T) {
        if (isLocked) {
            synchronized(toAddListenerSet) { select(listener).addTo(toAddListenerSet) }
        } else {
            synchronized(listenerSet) { select(listener).addTo(listenerSet) }
        }
    }

    @Synchronized
    fun removeListener(listener: T) {
        if (isLocked) {
            synchronized(toRemoveListenerSet) { select(listener).addTo(toRemoveListenerSet) }
        } else {
            synchronized(listenerSet) { select(listener).removeFrom(listenerSet) }
        }
    }

    @Synchronized
    fun get(): Set<T> {
        return listenerSet
    }

    @Synchronized
    fun lock(): Boolean {
        if (isLocked) {
            return false
        }
        isLocked = true
        return true
    }

    @Synchronized
    fun release() {
        isLocked = false
        // populate while locked
        synchronized(toRemoveListenerSet) {
            for (t in toRemoveListenerSet) {
                removeListener(t)
            }
            toRemoveListenerSet.clear()
        }
        synchronized(toAddListenerSet) {
            for (t in toAddListenerSet) {
                addListener(t)
            }
            toAddListenerSet.clear()
        }
    }

    private fun select(listener: T): Edit {
        return Edit(listener)
    }

    inner class Edit(private val listener: T) {
        fun addTo(listenerSet: MutableSet<T>) {
            listenerSet.add(listener)
        }

        fun removeFrom(listenerSet: MutableSet<T>) {
            listenerSet.remove(listener)
        }
    }
}