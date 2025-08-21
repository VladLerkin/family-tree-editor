package com.pedigree.editor;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Tracks selected entity IDs. Accepts either a String ID or an object exposing getId().
 */
public class SelectionModel {
    public interface BoundsProvider {
        /**
         * Returns a bounding rectangle (in layout coordinates) for a given node id.
         */
        Rect getBounds(String id);

        /**
         * Returns all selectable node ids.
         */
        Set<String> getAllIds();
    }

    private final LinkedHashSet<String> selectedIds = new LinkedHashSet<>();
    private BoundsProvider boundsProvider;

    public void setBoundsProvider(BoundsProvider provider) {
        this.boundsProvider = provider;
    }

    public void select(Object obj) {
        Objects.requireNonNull(obj, "obj");
        String id = resolveId(obj);
        selectedIds.clear();
        selectedIds.add(id);
    }

    public void addToSelection(Object obj) {
        Objects.requireNonNull(obj, "obj");
        selectedIds.add(resolveId(obj));
    }

    public void selectRange(Rect rect) {
        if (rect == null || boundsProvider == null) return;
        selectedIds.clear();
        for (String id : boundsProvider.getAllIds()) {
            Rect b = boundsProvider.getBounds(id);
            if (b != null && rect.intersects(b)) {
                selectedIds.add(id);
            }
        }
    }

    public void clear() {
        selectedIds.clear();
    }

    public boolean isSelected(String id) {
        return selectedIds.contains(id);
    }

    public Set<String> getSelectedIds() {
        return Collections.unmodifiableSet(selectedIds);
    }

    private static String resolveId(Object obj) {
        if (obj instanceof String s) return s;
        try {
            Method m = obj.getClass().getMethod("getId");
            Object val = m.invoke(obj);
            if (val instanceof String s) return s;
        } catch (Exception ignored) { }
        throw new IllegalArgumentException("Selection requires String id or object exposing getId()");
    }
}
