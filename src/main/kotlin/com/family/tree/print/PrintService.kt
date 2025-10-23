package com.family.tree.print

import javafx.print.PrinterJob
import javafx.scene.Node

/**
 * Kotlin version of PrintService. Preserves Java static-style API via @JvmStatic.
 */
object PrintService {
    @JvmStatic
    fun printNode(node: Node?) {
        if (node == null) return
        val job = PrinterJob.createPrinterJob()
        if (job != null && job.showPrintDialog(node.scene.window)) {
            val success = job.printPage(node)
            if (success) job.endJob()
        }
    }
}
