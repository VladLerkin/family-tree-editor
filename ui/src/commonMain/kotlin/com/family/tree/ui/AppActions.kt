package com.family.tree.ui

/**
 * Global action endpoints that Desktop menu and other top-level UI can trigger.
 * MainScreen assigns these lambdas during composition. Defaults are no-ops.
 * 
 * Implementation uses private backing fields with public methods to ensure
 * menu items always call the latest assigned action (fixes macOS system menu issue).
 */
object AppActions {
    // File - backing fields
    private var _newProject: () -> Unit = {}
    private var _openPed: () -> Unit = {}
    private var _savePed: () -> Unit = {}
    private var _importRel: () -> Unit = {}
    private var _importGedcom: () -> Unit = {}
    private var _importAiText: () -> Unit = {}
    private var _voiceInput: () -> Unit = {}
    private var _exportGedcom: () -> Unit = {}
    private var _exportSvgCurrent: () -> Unit = {}
    private var _exportSvgFit: () -> Unit = {}
    private var _exportPngCurrent: () -> Unit = {}
    private var _exportPngFit: () -> Unit = {}

    // View - backing fields
    private var _zoomIn: () -> Unit = {}
    private var _zoomOut: () -> Unit = {}
    private var _reset: () -> Unit = {}

    // Edit - backing fields
    private var _manageSources: () -> Unit = {}

    // Help - backing fields
    private var _showAbout: () -> Unit = {}
    private var _showAiSettings: () -> Unit = {}
    private var _exit: () -> Unit = {}

    // File - public setters and call methods
    var newProject: () -> Unit
        get() = { _newProject() }
        set(value) { _newProject = value }
    
    var openPed: () -> Unit
        get() = { _openPed() }
        set(value) { _openPed = value }
    
    var savePed: () -> Unit
        get() = { _savePed() }
        set(value) { _savePed = value }
    
    var importRel: () -> Unit
        get() = { _importRel() }
        set(value) { _importRel = value }
    
    var importGedcom: () -> Unit
        get() = { _importGedcom() }
        set(value) { _importGedcom = value }
    
    var importAiText: () -> Unit
        get() = { _importAiText() }
        set(value) { _importAiText = value }
    
    var voiceInput: () -> Unit
        get() = { _voiceInput() }
        set(value) { _voiceInput = value }
    
    var exportGedcom: () -> Unit
        get() = { _exportGedcom() }
        set(value) { _exportGedcom = value }
    
    var exportSvgCurrent: () -> Unit
        get() = { _exportSvgCurrent() }
        set(value) { _exportSvgCurrent = value }
    
    var exportSvgFit: () -> Unit
        get() = { _exportSvgFit() }
        set(value) { _exportSvgFit = value }
    
    var exportPngCurrent: () -> Unit
        get() = { _exportPngCurrent() }
        set(value) { _exportPngCurrent = value }
    
    var exportPngFit: () -> Unit
        get() = { _exportPngFit() }
        set(value) { _exportPngFit = value }

    // View - public setters and call methods
    var zoomIn: () -> Unit
        get() = { _zoomIn() }
        set(value) { _zoomIn = value }
    
    var zoomOut: () -> Unit
        get() = { _zoomOut() }
        set(value) { _zoomOut = value }
    
    var reset: () -> Unit
        get() = { _reset() }
        set(value) { _reset = value }

    // Edit - public setters and call methods
    var manageSources: () -> Unit
        get() = { _manageSources() }
        set(value) { _manageSources = value }

    // Help - public setters and call methods
    var showAbout: () -> Unit
        get() = { _showAbout() }
        set(value) { _showAbout = value }
    
    var showAiSettings: () -> Unit
        get() = { _showAiSettings() }
        set(value) { _showAiSettings = value }
    
    var exit: () -> Unit
        get() = { _exit() }
        set(value) { _exit = value }
}
