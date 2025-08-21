package com.pedigree.render;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Holds the current set of selected ids for rendering highlight.
 * This is a simple global bridge between selection sources and renderer.
 */
public final class RenderHighlightState {
    private static volatile Set<String> selectedIds = Collections.emptySet();

    private RenderHighlightState() { }

    public static void setSelectedIds(Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            selectedIds = Collections.emptySet();
        } else {
            selectedIds = Collections.unmodifiableSet(new HashSet<>(ids));
        }
    }

    public static Set<String> getSelectedIds() {
        return selectedIds;
    }
}
