package com.pedigree.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class BackupService {
    private BackupService() {}

    public static void rotateBackups(Path projectFilePath) throws IOException {
        rotateBackups(projectFilePath, 5);
    }

    public static void rotateBackups(Path projectFilePath, int maxBackups) throws IOException {
        if (maxBackups <= 0) {
            return;
        }

        // Delete the oldest backup if present
        Path oldest = backupPath(projectFilePath, maxBackups);
        if (Files.exists(oldest)) {
            Files.delete(oldest);
        }

        // Shift existing backups: .bakN-1 -> .bakN down to .bak1
        for (int i = maxBackups - 1; i >= 1; i--) {
            Path src = backupPath(projectFilePath, i);
            if (Files.exists(src)) {
                Path dst = backupPath(projectFilePath, i + 1);
                Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        // Create new .bak1 from the current project file
        if (Files.exists(projectFilePath)) {
            Path firstBackup = backupPath(projectFilePath, 1);
            Files.copy(projectFilePath, firstBackup, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Path backupPath(Path original, int index) {
        String fileName = original.getFileName().toString();
        String backupName = fileName + ".bak" + index;
        return original.resolveSibling(backupName);
    }
}



