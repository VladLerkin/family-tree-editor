package com.family.tree.ui

import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer

/**
 * Simple UI-level selection bus to propagate selected entity id
 * (e.g., from canvas to list views).
 */
object SelectionBus {
    private val LISTENERS: MutableList<Consumer<String>> = CopyOnWriteArrayList()

    @JvmStatic
    fun addListener(listener: Consumer<String>?) {
        if (listener != null) {
            LISTENERS.add(listener)
        }
    }

    @JvmStatic
    fun removeListener(listener: Consumer<String>?) {
        if (listener != null) LISTENERS.remove(listener)
    }

    @JvmStatic
    fun publish(id: String) {
        for (l in LISTENERS) {
            try {
                l.accept(id)
            } catch (_: Throwable) {
                // avoid breaking the chain
            }
        }
    }
}
