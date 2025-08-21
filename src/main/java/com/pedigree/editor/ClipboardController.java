package com.pedigree.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Clipboard operations using a simple data adapter.
 * Operates on IDs provided by SelectionModel.
 */
public class ClipboardController {

    public interface DataAdapter {
        /**
         * Create a duplicated entity based on the source ID and return the new entity ID.
         */
        String duplicate(String sourceEntityId);

        /**
         * Remove (cut) an entity by ID.
         */
        void remove(String entityId);
    }

    private final SelectionModel selectionModel;
    private final DataAdapter dataAdapter;
    private final List<String> clipboardIds = new ArrayList<>();

    public ClipboardController(SelectionModel selectionModel, DataAdapter dataAdapter) {
        this.selectionModel = Objects.requireNonNull(selectionModel, "selectionModel");
        this.dataAdapter = Objects.requireNonNull(dataAdapter, "dataAdapter");
    }

    public List<String> copy() {
        clipboardIds.clear();
        clipboardIds.addAll(selectionModel.getSelectedIds());
        return List.copyOf(clipboardIds);
    }

    public List<String> cut() {
        clipboardIds.clear();
        Set<String> ids = selectionModel.getSelectedIds();
        clipboardIds.addAll(ids);
        for (String id : ids) {
            dataAdapter.remove(id);
        }
        selectionModel.clear();
        return List.copyOf(clipboardIds);
    }

    public List<String> paste() {
        List<String> newIds = new ArrayList<>();
        for (String id : clipboardIds) {
            newIds.add(dataAdapter.duplicate(id));
        }
        return newIds;
    }
}
