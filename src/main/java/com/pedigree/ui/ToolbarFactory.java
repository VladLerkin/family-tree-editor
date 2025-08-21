package com.pedigree.ui;

import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;

public class ToolbarFactory {

    private final Runnable onNew;
    private final Runnable onOpen;
    private final Runnable onSave;
    private final Runnable onZoomIn;
    private final Runnable onZoomOut;
    private final Runnable onResetZoom;

    public ToolbarFactory(Runnable onNew, Runnable onOpen, Runnable onSave,
                          Runnable onZoomIn, Runnable onZoomOut, Runnable onResetZoom) {
        this.onNew = onNew;
        this.onOpen = onOpen;
        this.onSave = onSave;
        this.onZoomIn = onZoomIn;
        this.onZoomOut = onZoomOut;
        this.onResetZoom = onResetZoom;
    }

    public ToolBar create() {
        Button bNew = button("New", onNew);
        Button bOpen = button("Open", onOpen);
        Button bSave = button("Save", onSave);
        Button bZoomIn = button("Zoom +", onZoomIn);
        Button bZoomOut = button("Zoom -", onZoomOut);
        Button bResetZoom = button("Reset", onResetZoom);
        return new ToolBar(bNew, bOpen, bSave, bZoomIn, bZoomOut, bResetZoom);
    }

    private static Button button(String text, Runnable action) {
        Button b = new Button(text);
        b.setOnAction(e -> { if (action != null) action.run(); });
        return b;
    }
}
