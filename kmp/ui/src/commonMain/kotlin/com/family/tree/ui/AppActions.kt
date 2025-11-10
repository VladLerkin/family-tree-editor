package com.family.tree.ui

/**
 * Global action endpoints that Desktop menu and other top-level UI can trigger.
 * MainScreen assigns these lambdas during composition. Defaults are no-ops.
 */
object AppActions {
    // File
    var openPed: () -> Unit = {}
    var savePed: () -> Unit = {}
    var importRel: () -> Unit = {}
    var importGedcom: () -> Unit = {}
    var exportGedcom: () -> Unit = {}
    var exportSvgCurrent: () -> Unit = {}
    var exportSvgFit: () -> Unit = {}
    var exportPngCurrent: () -> Unit = {}
    var exportPngFit: () -> Unit = {}

    // View
    var toggleGrid: () -> Unit = {}
    var setLineWidth1x: () -> Unit = {}
    var setLineWidth2x: () -> Unit = {}
    var zoomIn: () -> Unit = {}
    var zoomOut: () -> Unit = {}
    var reset: () -> Unit = {}
}
