package com.pedigree.ui;

import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;

public class ToolbarFactory {

    private final Runnable onZoomIn;
    private final Runnable onZoomOut;

    public ToolbarFactory(Runnable onZoomIn, Runnable onZoomOut) {
        this.onZoomIn = onZoomIn;
        this.onZoomOut = onZoomOut;
    }

    public ToolBar create() {
        Button bZoomIn = button("Zoom +", onZoomIn);
        Button bZoomOut = button("Zoom -", onZoomOut);
        return new ToolBar(bZoomIn, bZoomOut);
    }

    private static Button button(String text, Runnable action) {
        Button b = new Button(text);
        b.setOnAction(e -> { if (action != null) action.run(); });
        return b;
    }
}
