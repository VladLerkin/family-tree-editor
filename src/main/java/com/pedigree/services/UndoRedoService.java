package com.pedigree.services;

import com.pedigree.editor.Command;
import com.pedigree.editor.CommandStack;
import com.pedigree.model.ProjectMetadata;

public class UndoRedoService {
    private final CommandStack commandStack = new CommandStack();
    private ProjectMetadata projectMetadata;

    public void bindProjectMetadata(ProjectMetadata meta) { this.projectMetadata = meta; }

    public void execute(Command command) { commandStack.execute(command); touch(); }
    public void undo() { commandStack.undo(); touch(); }
    public void redo() { commandStack.redo(); touch(); }
    public boolean canUndo() { return commandStack.canUndo(); }
    public boolean canRedo() { return commandStack.canRedo(); }
    public int getUndoCount() { return commandStack.getUndoSize(); }
    public int getRedoCount() { return commandStack.getRedoSize(); }

    private void touch() {
        if (projectMetadata != null) {
            projectMetadata.setModifiedAt(java.time.Instant.now());
        }
    }
}


