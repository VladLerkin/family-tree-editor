package com.family.tree.ui

import com.family.tree.editor.AlignAndDistributeController
import com.family.tree.editor.CanvasView
import com.family.tree.gedcom.GedcomExporter
import com.family.tree.gedcom.GedcomImporter
import com.family.tree.layout.TreeLayoutEngine
import com.family.tree.render.NodeMetrics
import com.family.tree.services.ProjectService
import com.family.tree.services.UndoRedoService
import javafx.geometry.Orientation
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import java.nio.file.Path
import java.util.*

class MainWindow {
    private val projectService: ProjectService
    private val undoRedoService: UndoRedoService

    // Convenience constructor
    constructor(projectService: ProjectService) : this(projectService, UndoRedoService())

    constructor(projectService: ProjectService, undoRedoService: UndoRedoService) {
        this.projectService = projectService
        this.undoRedoService = undoRedoService

        canvasView.dirtyCallback = Runnable { markDirty() }
        canvasPane.setOnSelectionChanged { ids -> onSelectionChanged(ids) }

        // List selection -> canvas selection + open properties
        individualsList.setOnSelect { id ->
            if (id != null) {
                canvasView.select(id)
                canvasPane.draw()
                propertiesInspector.setSelection(setOf(id))
            }
        }
        familiesList.setOnSelect { familyId ->
            if (familyId == null) return@setOnSelect
            val data = projectService.currentData
            if (data == null || data.families == null) return@setOnSelect
            data.families.stream().filter { f -> familyId == f.id }.findFirst().ifPresent { fam ->
                val ids = LinkedHashSet<String>()
                fam.husbandId?.takeIf { it.isNotBlank() }?.let { ids.add(it) }
                fam.wifeId?.takeIf { it.isNotBlank() }?.let { ids.add(it) }
                fam.childrenIds?.forEach { cid -> if (!cid.isNullOrBlank()) ids.add(cid) }
                canvasPane.selectAndHighlight(ids)
                propertiesInspector.setSelection(setOf(familyId))
            }
        }
        relationshipsList.setOnSelect { _ -> canvasPane.draw() }

        // Families add/edit/delete
        familiesList.setOnAdd {
            val dlg = FamilyDialog()
            val data = projectService.currentData
            dlg.showCreate(data).ifPresent { fam ->
                data!!.families.add(fam)
                fam.husbandId?.let {
                    val r = com.family.tree.model.Relationship()
                    r.type = com.family.tree.model.Relationship.Type.SPOUSE_TO_FAMILY
                    r.fromId = it
                    r.toId = fam.id
                    data.relationships.add(r)
                }
                fam.wifeId?.let {
                    val r = com.family.tree.model.Relationship()
                    r.type = com.family.tree.model.Relationship.Type.SPOUSE_TO_FAMILY
                    r.fromId = it
                    r.toId = fam.id
                    data.relationships.add(r)
                }
                for (cid in fam.childrenIds) {
                    val r = com.family.tree.model.Relationship()
                    r.type = com.family.tree.model.Relationship.Type.FAMILY_TO_CHILD
                    r.fromId = fam.id
                    r.toId = cid
                    data.relationships.add(r)
                }
                refreshAll()
                statusBar.text = "Added family"
            }
        }
        familiesList.setOnEdit { id ->
            val data = projectService.currentData ?: return@setOnEdit
            data.families.stream().filter { f -> f.id == id }.findFirst().ifPresent { fam ->
                val dlg = FamilyDialog()
                if (dlg.showEdit(fam, data)) {
                    data.relationships.removeIf { r -> id == r.fromId || id == r.toId }
                    fam.husbandId?.let {
                        val r = com.family.tree.model.Relationship()
                        r.type = com.family.tree.model.Relationship.Type.SPOUSE_TO_FAMILY
                        r.fromId = it
                        r.toId = fam.id
                        data.relationships.add(r)
                    }
                    fam.wifeId?.let {
                        val r = com.family.tree.model.Relationship()
                        r.type = com.family.tree.model.Relationship.Type.SPOUSE_TO_FAMILY
                        r.fromId = it
                        r.toId = fam.id
                        data.relationships.add(r)
                    }
                    for (cid in fam.childrenIds) {
                        val r = com.family.tree.model.Relationship()
                        r.type = com.family.tree.model.Relationship.Type.FAMILY_TO_CHILD
                        r.fromId = fam.id
                        r.toId = cid
                        data.relationships.add(r)
                    }
                    refreshAll()
                    statusBar.text = "Edited family"
                }
            }
        }
        familiesList.setOnDelete { id ->
            val data = projectService.currentData ?: return@setOnDelete
            data.families.removeIf { f -> f.id == id }
            data.relationships.removeIf { r -> id == r.fromId || id == r.toId }
            refreshAll()
            statusBar.text = "Deleted family"
        }

        // Individuals add/edit/delete
        individualsList.setOnAdd {
            val dlg = IndividualDialog()
            dlg.showCreate().ifPresent { ind ->
                val d = projectService.currentData
                d!!.individuals.add(ind)
                refreshAll()
                statusBar.text = "Added individual: ${ind.firstName} ${ind.lastName}"
            }
        }
        individualsList.setOnEdit { id ->
            val data = projectService.currentData ?: return@setOnEdit
            data.individuals.stream().filter { i -> i.id == id }.findFirst().ifPresent { ind ->
                val dlg = IndividualDialog()
                if (dlg.showEdit(ind)) {
                    refreshAll()
                    statusBar.text = "Edited individual: ${ind.firstName} ${ind.lastName}"
                }
            }
        }
        individualsList.setOnDelete { id ->
            val data = projectService.currentData ?: return@setOnDelete
            data.individuals.removeIf { i -> i.id == id }
            data.relationships.removeIf { r -> id == r.fromId || id == r.toId }
            refreshAll()
            statusBar.text = "Deleted individual"
        }
    }

    private val canvasView = CanvasView()
    private val canvasPane = CanvasPane(canvasView)
    private val individualsList = IndividualsListView()
    private val familiesList = FamiliesListView()
    private val relationshipsList = RelationshipsListView()
    private val propertiesInspector = PropertiesInspector()

    private val layoutEngine = TreeLayoutEngine()
    private val metrics: com.family.tree.render.TextAwareNodeMetrics = com.family.tree.render.TextAwareNodeMetrics()

    private val statusBar = Label("Ready")

    fun show(stage: Stage) {
        val root = BorderPane()

        val menuFactory = MenuBarFactory(
            this::newProject,
            this::openProject,
            this::saveProject,
            this::saveProjectAs,
            this::exitApp,
            this::importGedcom,
            this::importRel,
            this::exportGedcom,
            this::exportHtml,
            this::exportSvg,
            this::exportImage,
            this::printCanvas,
            this::undo,
            this::redo,
            this::copySelection,
            this::cutSelection,
            this::pasteClipboard,
            this::deleteSelection,
            this::zoomIn,
            this::zoomOut,
            this::resetZoom,
            { align(AlignAndDistributeController.Alignment.TOP) },
            { align(AlignAndDistributeController.Alignment.MIDDLE) },
            { align(AlignAndDistributeController.Alignment.BOTTOM) },
            { align(AlignAndDistributeController.Alignment.LEFT) },
            { align(AlignAndDistributeController.Alignment.CENTER) },
            { align(AlignAndDistributeController.Alignment.RIGHT) },
            { distribute(AlignAndDistributeController.Distribution.HORIZONTAL) },
            { distribute(AlignAndDistributeController.Distribution.VERTICAL) },
            this::openQuickSearch,
            this::debugExportRelSection,
            this::manageSources,
            this::showAbout,
            java.util.function.Supplier { projectService.recentProjects },
            this::openProject
        )
        root.top = menuFactory.create()

        val toolbarFactory = ToolbarFactory(
            this::zoomIn,
            this::zoomOut
        )
        root.left = null
        root.bottom = statusBar

        val leftTabs = TabPane()
        leftTabs.tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
        leftTabs.tabs.add(Tab("Individuals", individualsList.getView()))
        leftTabs.tabs.add(Tab("Families", familiesList.getView()))
        leftTabs.tabs.add(Tab("Relationships", relationshipsList.getView()))

        val centerSplit = SplitPane()
        centerSplit.orientation = Orientation.HORIZONTAL
        centerSplit.items.addAll(leftTabs, canvasPane.getView(), propertiesInspector.getView())
        centerSplit.setDividerPositions(0.2, 0.8)

        val centerPane = BorderPane()
        centerPane.top = toolbarFactory.create()
        centerPane.center = centerSplit

        root.center = centerPane

        val scene = Scene(root, 1200.0, 800.0)
        stage.title = "Family Tree Editor"
        stage.scene = scene
        stage.show()

        newProject()
    }

    private fun newProject() {
        projectService.createNewProject()
        refreshAll(true)
        statusBar.text = "New project created"
    }

    private fun openProject() {
        val path = Dialogs.chooseOpenProjectPath() ?: return
        openProject(path)
    }

    private fun openProject(path: Path) {
        try {
            projectService.openProject(path)
            refreshAll()
            fitTreeToCanvas()
            statusBar.text = "Opened: $path"
        } catch (ex: Exception) {
            Dialogs.showError("Open Project Failed", ex.message)
        }
    }

    private fun saveProject() {
        try {
            if (projectService.currentProjectPath == null) {
                saveProjectAs(); return
            }
            persistViewport()
            persistNodePositions()
            projectService.saveProject()
            statusBar.text = "Saved: ${projectService.currentProjectPath}"
        } catch (ex: Exception) {
            Dialogs.showError("Save Project Failed", ex.message)
        }
    }

    private fun saveProjectAs() {
        val path = Dialogs.chooseSaveProjectPath() ?: return
        try {
            persistViewport()
            persistNodePositions()
            projectService.saveProjectAs(path)
            statusBar.text = "Saved As: $path"
        } catch (ex: Exception) {
            Dialogs.showError("Save As Failed", ex.message)
        }
    }

    private fun importGedcom() {
        val path = Dialogs.chooseOpenGedcomPath() ?: return
        try {
            if (projectService.currentData == null) {
                projectService.createNewProject()
            }
            val importer = GedcomImporter()
            val imported = importer.importFromFile(path)
            projectService.currentData!!.individuals.addAll(imported.individuals)
            projectService.currentData!!.families.addAll(imported.families)
            projectService.currentData!!.relationships.addAll(imported.relationships)
            if (!imported.sources.isNullOrEmpty()) projectService.currentData!!.sources.addAll(imported.sources)
            if (!imported.repositories.isNullOrEmpty()) projectService.currentData!!.repositories.addAll(imported.repositories)
            if (!imported.submitters.isNullOrEmpty()) projectService.currentData!!.submitters.addAll(imported.submitters)
            refreshAll()
            fitTreeToCanvas()
            statusBar.text = "Imported GEDCOM: $path"
        } catch (ex: Exception) {
            Dialogs.showError("Import GEDCOM Failed", ex.message)
        }
    }

    private fun importRel() {
        val path = Dialogs.chooseOpenRelPath() ?: return
        try {
            if (projectService.currentData == null) {
                projectService.createNewProject()
            }
            val importer = com.family.tree.rel.RelImporter()
            val imported = importer.importFromFileWithLayout(path, projectService.currentLayout)
            projectService.currentData!!.individuals.addAll(imported.individuals)
            projectService.currentData!!.families.addAll(imported.families)
            projectService.currentData!!.relationships.addAll(imported.relationships)
            if (!imported.sources.isNullOrEmpty()) projectService.currentData!!.sources.addAll(imported.sources)
            if (!imported.repositories.isNullOrEmpty()) projectService.currentData!!.repositories.addAll(imported.repositories)
            refreshAll()
            fitTreeToCanvas()
            statusBar.text = "Imported REL: $path"
        } catch (ex: Exception) {
            Dialogs.showError("Import REL Failed", ex.message)
        }
    }

    private fun exportGedcom() {
        val path = Dialogs.chooseSaveGedcomPath() ?: return
        try {
            val exporter = GedcomExporter()
            exporter.exportToFile(projectService.currentData, path)
            statusBar.text = "Exported GEDCOM: $path"
        } catch (ex: Exception) {
            Dialogs.showError("Export GEDCOM Failed", ex.message)
        }
    }

    private fun exportHtml() {
        val data = projectService.currentData
        val layout = canvasView.layout
        if (data == null || layout == null) {
            Dialogs.showError("Export HTML", "Nothing to export.")
            return
        }
        val path = Dialogs.chooseSaveHtmlPath() ?: return
        try {
            com.family.tree.export.HtmlExporter().exportToFile(data, layout, NodeMetrics(), path)
            statusBar.text = "Exported HTML: $path"
        } catch (ex: Exception) {
            Dialogs.showError("Export HTML Failed", ex.message)
        }
    }

    private fun exportSvg() {
        val data = projectService.currentData
        val layout = canvasView.layout
        if (data == null || layout == null) {
            Dialogs.showError("Export SVG", "Nothing to export.")
            return
        }
        val path = Dialogs.chooseSaveSvgPath() ?: return
        try {
            com.family.tree.export.SvgExporter().exportToFile(data, layout, NodeMetrics(), path)
            statusBar.text = "Exported SVG: $path"
        } catch (ex: Exception) {
            Dialogs.showError("Export SVG Failed", ex.message)
        }
    }

    private fun exportImage() {
        val data = projectService.currentData
        val layout = canvasView.layout
        if (data == null || layout == null) {
            Dialogs.showError("Export Image", "Nothing to export.")
            return
        }
        val path = Dialogs.chooseSaveImagePath() ?: return
        try {
            com.family.tree.export.ImageExporter().exportToFile(data, layout, NodeMetrics(), path)
            statusBar.text = "Exported Image: $path"
        } catch (ex: Exception) {
            Dialogs.showError("Export Image Failed", ex.message)
        }
    }

    private fun printCanvas() {
        try {
            com.family.tree.print.PrintService.printNode(canvasPane.getView())
        } catch (ex: Exception) {
            Dialogs.showError("Print Failed", ex.message)
        }
    }

    private fun exitApp() {
        val win = javafx.stage.Window.getWindows().firstOrNull { it.isShowing }
        if (win is Stage) win.close()
    }

    private fun undo() { undoRedoService.undo() }
    private fun redo() { undoRedoService.redo() }

    private fun zoomIn() { canvasView.zoomIn() }
    private fun zoomOut() { canvasView.zoomOut() }
    private fun resetZoom() { canvasView.setZoom(1.0); canvasPane.draw() }

    private fun copySelection() { statusBar.text = "Copy not implemented (placeholder)" }
    private fun cutSelection() { statusBar.text = "Cut not implemented (placeholder)" }
    private fun pasteClipboard() { statusBar.text = "Paste not implemented (placeholder)" }

    private fun deleteSelection() {
        val ids = canvasView.selectionModel.getSelectedIds()
        if (ids.isEmpty()) return
        val data = projectService.currentData ?: return
        data.individuals.removeIf { i -> ids.contains(i.id) }
        data.families.removeIf { f -> ids.contains(f.id) }
        data.relationships.removeIf { r -> ids.contains(r.fromId) || ids.contains(r.toId) }
        refreshAll()
        statusBar.text = "Deleted selected objects"
    }

    private fun align(mode: AlignAndDistributeController.Alignment) {
        val acc = getLayoutIdsAndAccessor() ?: return
        val controller = AlignAndDistributeController(canvasView.selectionModel, acc)
        controller.align(mode)
        canvasPane.draw()
        markDirty()
    }

    private fun distribute(mode: AlignAndDistributeController.Distribution) {
        val acc = getLayoutIdsAndAccessor() ?: return
        val controller = AlignAndDistributeController(canvasView.selectionModel, acc)
        controller.distribute(mode)
        canvasPane.draw()
        markDirty()
    }

    private fun getLayoutIdsAndAccessor(): AlignAndDistributeController.PositionAccessor? {
        val layoutResult = canvasView.layout ?: return null
        if (projectService.currentData == null) return null
        return object : AlignAndDistributeController.PositionAccessor {
            override fun getX(id: String): Double {
                val p = layoutResult.getPosition(id) ?: return 0.0
                return p.x
            }
            override fun getY(id: String): Double {
                val p = layoutResult.getPosition(id) ?: return 0.0
                return p.y
            }
            override fun getWidth(id: String): Double = metrics.getWidth(id)
            override fun getHeight(id: String): Double = metrics.getHeight(id)
            override fun setX(id: String, x: Double) { canvasView.moveNode(id, x, getY(id)) }
            override fun setY(id: String, y: Double) { canvasView.moveNode(id, getX(id), y) }
        }
    }

    private fun openQuickSearch() {
        val data = projectService.currentData ?: return
        QuickSearchDialog(data) { id ->
            canvasView.select(id)
            canvasPane.draw()
        }.show()
    }

    private fun showAbout() { AboutDialog.show() }

    private fun manageSources() {
        val data = projectService.currentData
        if (data == null) {
            Dialogs.showError("Sources", "Нет открытого проекта.")
            return
        }
        try {
            SourcesManagerDialog(data).showAndWait()
            refreshAll()
        } catch (ex: Throwable) {
            Dialogs.showError("Sources", ex.message)
        }
    }

    private fun debugExportRelSection() {
        val path = Dialogs.chooseOpenRelPath() ?: return
        val idDialog = TextInputDialog("P123")
        idDialog.title = "Debug: Export REL Section"
        idDialog.headerText = "Введите идентификатор персоны (например, P266)"
        idDialog.contentText = "P-id:"
        val res = idDialog.showAndWait()
        if (res.isEmpty) return
        val pid = res.get().trim().uppercase(Locale.ROOT)
        if (!pid.matches(Regex("P\\d{1,5}"))) {
            Dialogs.showError("Debug: Export REL Section", "Неверный формат идентификатора. Ожидается P и число, например P266.")
            return
        }
        try {
            val raw = java.nio.file.Files.readAllBytes(path)
            val cleaned = dropZeroBytes(raw)
            var text = String(cleaned, java.nio.charset.StandardCharsets.UTF_8)
            val startIdx = text.indexOf(pid)
            if (startIdx < 0) {
                Dialogs.showError("Debug: Export REL Section", "Не найден раздел $pid в выбранном файле.")
                return
            }
            var searchFrom = startIdx + pid.length
            val secRe = java.util.regex.Pattern.compile("(P\\d{1,5}|F\\d{1,5})")
            val m = secRe.matcher(text)
            var endIdx = text.length
            while (m.find()) {
                val idx = m.start(1)
                if (idx > startIdx) { endIdx = idx; break }
            }
            var section = text.substring(startIdx, maxOf(startIdx, endIdx))
            section = section.replace(Regex("[\\p{Cntrl}&&[^\\r\\n]]"), "")

            val out = StringBuilder()
            out.append(section)
            try {
                val OBJE_BLOCK_RE = java.util.regex.Pattern.compile("(?i)OBJE\\s*([\\s\\S]+?)\\s*(?=(OBJE|NOTE|NOTES|SOUR|SEX|BIRT|DEAT|FAMC|FAMS|SUBM|P\\d+|F\\d+|_X|_Y)|$)", java.util.regex.Pattern.DOTALL)
                val FORM_INNER_RE = java.util.regex.Pattern.compile("(?i)FORM\\s*([^\\r\\n\\s]+)")
                val TITL_INNER_RE = java.util.regex.Pattern.compile("(?i)TITL\\s*([\\s\\S]+?)\\s*(?=(FORM|FILE|TITL|OBJE|NOTE|NOTES|SOUR|P\\d+|F\\d+|_X|_Y)|$)", java.util.regex.Pattern.DOTALL)
                val FILE_INNER_RE = java.util.regex.Pattern.compile("(?i)FILE\\s*([\\s\\S]+?)\\s*(?=(FORM|FILE|TITL|OBJE|NOTE|NOTES|SOUR|P\\d+|F\\d+|_X|_Y)|$)", java.util.regex.Pattern.DOTALL)
                val mOb = OBJE_BLOCK_RE.matcher(section)
                var count = 0
                val mediaSummary = StringBuilder()
                while (mOb.find()) {
                    val chunk = mOb.group(1) ?: continue
                    var form: String? = null
                    var titl: String? = null
                    var file: String? = null
                    val mf = FORM_INNER_RE.matcher(chunk)
                    if (mf.find()) form = mf.group(1)
                    val mt = TITL_INNER_RE.matcher(chunk)
                    if (mt.find()) titl = mt.group(1)
                    val mfile = FILE_INNER_RE.matcher(chunk)
                    if (mfile.find()) file = mfile.group(1)
                    count++
                    mediaSummary.append("#").append(count).append(": ")
                    if (titl != null) mediaSummary.append("TITL=\"").append(titl.replace(Regex("[\\\\p{Cntrl}&&[^\\r\\n]]"), "").trim()).append("\" ")
                    if (form != null) mediaSummary.append("FORM=").append(form.replace(Regex("[\\\\p{Cntrl}]"), "").trim()).append(" ")
                    if (file != null) mediaSummary.append("FILE=").append(file.replace(Regex("[\\\\p{Cntrl}]"), "").trim())
                    mediaSummary.append("\n")
                }
                out.append("\n\n--- Parsed Media (OBJE) ---\n")
                if (count == 0) {
                    out.append("(none found)\n")
                } else {
                    out.append(mediaSummary)
                    out.append("Total: ").append(count).append("\n")
                }
                try {
                    val withoutObje = OBJE_BLOCK_RE.matcher(section).replaceAll(" ")
                    val mf2 = FILE_INNER_RE.matcher(withoutObje)
                    var fileCount = 0
                    val stand = StringBuilder()
                    while (mf2.find()) {
                        var file = mf2.group(1)
                        if (file != null) file = file.replace(Regex("[\\p{Cntrl}]"), "").trim()
                        val start = maxOf(0, mf2.start() - 200)
                        val win = withoutObje.substring(start, mf2.start())
                        var titl: String? = null
                        val mt2 = TITL_INNER_RE.matcher(win)
                        if (mt2.find()) titl = mt2.group(1)
                        if (titl != null) titl = titl.replace(Regex("[\\p{Cntrl}&&[^\\r\\n]]"), "").trim()
                        val end = minOf(withoutObje.length, mf2.end() + 100)
                        val winF = withoutObje.substring(mf2.end(), end)
                        var form: String? = null
                        val mfForm = FORM_INNER_RE.matcher(winF)
                        if (mfForm.find()) form = mfForm.group(1)
                        if (form != null) form = form.replace(Regex("[\\p{Cntrl}]"), "").trim()
                        fileCount++
                        stand.append("#").append(fileCount).append(": ")
                        if (!titl.isNullOrBlank()) stand.append("TITL=\"").append(titl).append("\" ")
                        if (!form.isNullOrBlank()) stand.append("FORM=").append(form).append(" ")
                        if (!file.isNullOrBlank()) stand.append("FILE=").append(file)
                        stand.append("\n")
                    }
                    out.append("\n--- Standalone Media Tokens ---\n")
                    if (fileCount == 0) {
                        out.append("(none found)\n")
                    } else {
                        out.append(stand)
                        out.append("Total FILE tokens: ").append(fileCount).append("\n")
                    }
                } catch (_: Throwable) {}
            } catch (_: Throwable) {}

            val ta = TextArea(out.toString())
            ta.isEditable = false
            ta.isWrapText = false
            ta.prefColumnCount = 80
            ta.prefRowCount = 30
            val alert = Alert(Alert.AlertType.INFORMATION)
            alert.title = "REL Section: $pid"
            alert.headerText = "Очищенный текстовый фрагмент секции $pid (сводка медиа)"
            alert.dialogPane.content = ta
            alert.showAndWait()
        } catch (ex: Exception) {
            Dialogs.showError("Debug: Export REL Section", ex.message)
        }
    }

    private fun dropZeroBytes(input: ByteArray): ByteArray {
        val out = java.io.ByteArrayOutputStream(input.size)
        for (b in input) if (b.toInt() != 0) out.write(b.toInt())
        return out.toByteArray()
    }

    private fun refreshAll() = refreshAll(false)

    private fun refreshAll(isNewProject: Boolean) {
        val data = projectService.currentData
        canvasView.projectData = data
        canvasView.renderer = com.family.tree.render.TreeRenderer(metrics)
        canvasView.nodeMetrics = metrics
        metrics.setData(data)

        val computed = layoutEngine.computeLayout(data)

        val persisted = projectService.currentLayout
        if (persisted != null && persisted.nodePositions != null) {
            var centers = false
            try { centers = persisted.isPositionsAreCenters() } catch (_: Throwable) {}
            for ((id, np) in persisted.nodePositions) {
                if (np != null) {
                    var x = np.x
                    var y = np.y
                    if (centers) {
                        val w = metrics.getWidth(id)
                        val h = metrics.getHeight(id)
                        x -= w / 2.0
                        y -= h / 2.0
                    }
                    computed.setPosition(id, x, y)
                }
            }
            if (centers) {
                val map = persisted.nodePositions
                val xs = ArrayList<Double>()
                for (v in map.values) if (v != null) xs.add(v.x)
                var minDx = Double.POSITIVE_INFINITY
                for (i in 0 until xs.size) for (j in i + 1 until xs.size) {
                    val dx = kotlin.math.abs(xs[i] - xs[j])
                    if (dx > 0 && dx < minDx) minDx = dx
                }
                var scale = 1.0
                var targetWidth = 0.0
                for (id in map.keys) targetWidth += metrics.getWidth(id)
                if (map.isNotEmpty()) targetWidth /= map.size
                if (minDx != Double.POSITIVE_INFINITY && minDx > 0.0 && targetWidth > 0.0) {
                    scale = kotlin.math.max(1.0, kotlin.math.min(3.0, (targetWidth * 1.05) / minDx))
                }
                for ((id, np) in map) {
                    if (np != null) {
                        val w = metrics.getWidth(id)
                        val h = metrics.getHeight(id)
                        np.x = np.x * scale - w / 2.0
                        np.y = np.y * scale - h / 2.0
                    }
                }
                for ((id, np) in map) {
                    if (np != null) computed.setPosition(id, np.x, np.y)
                }
                persisted.positionsAreCenters = false
            }
            val hasViewport = persisted.zoom != 1.0 || persisted.viewOriginX != 0.0 || persisted.viewOriginY != 0.0
            if (hasViewport) {
                canvasView.setZoom(persisted.zoom)
                val dx = persisted.viewOriginX - canvasView.zoomAndPan.panX
                val dy = persisted.viewOriginY - canvasView.zoomAndPan.panY
                if (dx != 0.0 || dy != 0.0) canvasView.panBy(dx, dy)
            } else {
                if (isNewProject) {
                    canvasView.setZoom(1.0)
                } else {
                    // preserve current zoom
                }
            }
        }

        canvasView.layout = computed
        canvasPane.draw()

        individualsList.setData(data!!)
        familiesList.setData(data!!)
        relationshipsList.setData(data!!)
        propertiesInspector.setProjectContext(data, projectService.currentProjectPath)
        propertiesInspector.setSelection(setOf())
    }

    private fun onSelectionChanged(selectedIds: Set<String>) {
        propertiesInspector.setSelection(selectedIds)
    }

    private fun fitTreeToCanvas() {
        val layout = canvasView.layout
        if (layout != null && layout.getNodeIds().isNotEmpty()) {
            // Calculate bounding box
            var minX = Double.POSITIVE_INFINITY
            var minY = Double.POSITIVE_INFINITY
            var maxX = Double.NEGATIVE_INFINITY
            var maxY = Double.NEGATIVE_INFINITY
            val ids: Set<String> = layout.getNodeIds()
            for (id in ids) {
                val p = layout.getPosition(id) ?: continue
                val x = p.x
                val y = p.y
                val w = metrics.getWidth(id)
                val h = metrics.getHeight(id)
                minX = kotlin.math.min(minX, x)
                minY = kotlin.math.min(minY, y)
                maxX = kotlin.math.max(maxX, x + w)
                maxY = kotlin.math.max(maxY, y + h)
            }
            
            val fitZoom = computeFitToCanvasZoom(layout, metrics)
            canvasView.setZoom(fitZoom)
            
            // Center the tree in the canvas
            val canvasWidth = canvasPane.getView().width
            val canvasHeight = canvasPane.getView().height
            val treeWidth = (maxX - minX) * fitZoom
            val treeHeight = (maxY - minY) * fitZoom
            val panX = (canvasWidth - treeWidth) / 2.0 - minX * fitZoom
            val panY = (canvasHeight - treeHeight) / 2.0 - minY * fitZoom
            
            // Reset pan to centered position
            canvasView.zoomAndPan.panBy(-canvasView.zoomAndPan.panX, -canvasView.zoomAndPan.panY)
            canvasView.zoomAndPan.panBy(panX, panY)
            
            canvasPane.draw()
        }
    }

    private fun computeFitToCanvasZoom(layout: com.family.tree.layout.LayoutResult, metrics: NodeMetrics): Double {
        var minX = Double.POSITIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        val ids: Set<String> = layout.getNodeIds()
        for (id in ids) {
            val p = layout.getPosition(id) ?: continue
            val x = p.x
            val y = p.y
            val w = metrics.getWidth(id)
            val h = metrics.getHeight(id)
            minX = kotlin.math.min(minX, x)
            minY = kotlin.math.min(minY, y)
            maxX = kotlin.math.max(maxX, x + w)
            maxY = kotlin.math.max(maxY, y + h)
        }
        if (ids.isEmpty() || minX == Double.POSITIVE_INFINITY) return 1.0
        val canvasWidth = canvasPane.getView().width
        val canvasHeight = canvasPane.getView().height
        if (canvasWidth <= 0 || canvasHeight <= 0) return 1.0
        val margin = 40.0
        val treeWidth = maxX - minX
        val treeHeight = maxY - minY
        var zoom = kotlin.math.min((canvasWidth - 2 * margin) / treeWidth, (canvasHeight - 2 * margin) / treeHeight)
        val minZoom = canvasView.zoomAndPan.minZoom
        val maxZoom = canvasView.zoomAndPan.maxZoom
        zoom = kotlin.math.max(minZoom, kotlin.math.min(maxZoom, zoom))
        return zoom
    }

    private fun markDirty() {
        projectService.currentMeta?.let { meta ->
            undoRedoService.bindProjectMetadata(meta)
        }
    }

    private fun persistViewport() {
        val layout = projectService.currentLayout ?: return
        layout.zoom = canvasView.zoomAndPan.zoom
        layout.viewOriginX = canvasView.zoomAndPan.panX
        layout.viewOriginY = canvasView.zoomAndPan.panY
    }

    private fun persistNodePositions() {
        val layoutResult = canvasView.layout ?: return
        val persisted = projectService.currentLayout ?: return
        val map = persisted.nodePositions
        map.clear()
        for (id in layoutResult.getNodeIds()) {
            val p = layoutResult.getPosition(id) ?: continue
            val np = com.family.tree.model.ProjectLayout.NodePos()
            np.x = p.x
            np.y = p.y
            map[id] = np
        }
    }
}
