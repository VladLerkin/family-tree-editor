package com.pedigree.ui;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Simple UI-level selection bus to propagate selected entity id
 * (e.g., from canvas to list views).
 */
public final class SelectionBus {
    private static final List<Consumer<String>> LISTENERS = new CopyOnWriteArrayList<>();

    private SelectionBus() {}

    public static void addListener(Consumer<String> listener) {
        if (listener != null) {
            LISTENERS.add(listener);
        }
    }

    public static void removeListener(Consumer<String> listener) {
        LISTENERS.remove(listener);
    }

    public static void publish(String id) {
        for (Consumer<String> l : LISTENERS) {
            try {
                l.accept(id);
            } catch (Throwable ignored) {
                // avoid breaking the chain
            }
        }
    }
}
