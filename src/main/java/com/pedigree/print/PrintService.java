package com.pedigree.print;

import javafx.print.PrinterJob;
import javafx.scene.Node;

public class PrintService {
    public static void printNode(Node node) {
        if (node == null) return;
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(node.getScene().getWindow())) {
            boolean success = job.printPage(node);
            if (success) job.endJob();
        }
    }
}
