package com.pedigree.editor.commands;

import com.pedigree.editor.Command;
import com.pedigree.layout.LayoutResult;

public class MoveNodeCommand implements Command {
    private final LayoutResult layout;
    private final String nodeId;
    private final double oldX;
    private final double oldY;
    private final double newX;
    private final double newY;
    private final Runnable onChanged;

    public MoveNodeCommand(LayoutResult layout, String nodeId, double oldX, double oldY, double newX, double newY, Runnable onChanged) {
        this.layout = layout;
        this.nodeId = nodeId;
        this.oldX = oldX;
        this.oldY = oldY;
        this.newX = newX;
        this.newY = newY;
        this.onChanged = onChanged;
    }

    @Override
    public void execute() {
        layout.setPosition(nodeId, newX, newY);
        if (onChanged != null) onChanged.run();
    }

    @Override
    public void undo() {
        layout.setPosition(nodeId, oldX, oldY);
        if (onChanged != null) onChanged.run();
    }
}
