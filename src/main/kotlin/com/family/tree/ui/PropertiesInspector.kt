package com.family.tree.ui

import com.family.tree.model.Family
import com.family.tree.model.GedcomEvent
import com.family.tree.model.Individual
import com.family.tree.model.MediaAttachment
import com.family.tree.model.Note
import com.family.tree.model.Source
import com.family.tree.model.SourceCitation
import com.family.tree.model.Tag
import com.family.tree.services.MediaManager
import com.family.tree.services.MediaService
import com.family.tree.services.TagService
import com.family.tree.storage.ProjectRepository
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

class PropertiesInspector {
    private val root = VBox(10.0)
    private val scroller = ScrollPane()

    private var data: ProjectRepository.ProjectData? = null
    private var projectFilePath: Path? = null

    // Header
    private val header = Label("Properties")
    private val selectionInfo = Label("(no selection)")

    // Tags
    private val tagList = ListView<Tag>()
    private val tagInput = TextField()
    private val addTagBtn = Button("+")
    private val removeTagBtn = Button("-")

    // Notes
    private val noteList = ListView<Note>()
    private val noteEditor = TextArea()
    private val noteSourcesList = ListView<SourceCitation>()
    private val noteSourcePageField = TextField()
    private val addNoteBtn = Button("+")
    private val removeNoteBtn = Button("-")
    private val addNoteSourceBtn = Button("+")
    private val removeNoteSourceBtn = Button("-")
    private val manageNoteSourcesBtn = Button("...")

    // Media
    private val mediaList = ListView<MediaAttachment>()
    private val addMediaBtn = Button("+")
    private val editMediaBtn = Button("Edit...")
    private val openMediaBtn = Button("Open")
    private val removeMediaBtn = Button("-")

    // Events
    private val eventsList = ListView<GedcomEvent>()
    private val evTypeField = TextField()
    private val evDateField = TextField()
    private val evPlaceField = TextField()
    private val evSourcesList = ListView<SourceCitation>()
    private val evSourcePageField = TextField()
    private val addEventBtn = Button("+")
    private val removeEventBtn = Button("-")
    private val addEvSourceBtn = Button("+")
    private val removeEvSourceBtn = Button("-")
    private val manageSourcesBtn = Button("...")

    // Layout helpers
    private val eventsPane = TitledPane("Events (GEDCOM)", buildEventsPane())
    private val tagsPane = TitledPane("Tags", buildTagsPane())
    private val notesPane = TitledPane("Notes", buildNotesPane())
    private val mediaPane = TitledPane("Media", buildMediaPane())

    private var currentId: String? = null // selected entity id

    init {
        // Configure scrollable container
        root.padding = Insets(10.0)
        root.isFillWidth = true
        scroller.content = root
        scroller.isFitToWidth = true
        scroller.hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        scroller.vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED

        root.children.addAll(header, selectionInfo, eventsPane, tagsPane, notesPane, mediaPane)
        eventsPane.isExpanded = true
        tagsPane.isExpanded = false
        notesPane.isExpanded = false
        mediaPane.isExpanded = false

        // Events selection listeners
        eventsList.selectionModel.selectedItemProperty().addListener { _, _, n ->
            if (n != null) {
                evTypeField.text = n.type ?: ""
                evDateField.text = n.date ?: ""
                evPlaceField.text = n.place ?: ""
                evSourcesList.items.setAll(n.sources)
            } else {
                evTypeField.clear()
                evDateField.clear()
                evPlaceField.clear()
                evSourcesList.items.clear()
            }
        }
        evTypeField.textProperty().addListener { _, _, n ->
            val ev = eventsList.selectionModel.selectedItem
            if (ev != null) { ev.type = n?.trim(); eventsList.refresh() }
        }
        evDateField.textProperty().addListener { _, _, n ->
            val ev = eventsList.selectionModel.selectedItem
            if (ev != null) { ev.date = n?.trim(); eventsList.refresh() }
        }
        evPlaceField.textProperty().addListener { _, _, n ->
            val ev = eventsList.selectionModel.selectedItem
            if (ev != null) { ev.place = n?.trim(); eventsList.refresh() }
        }
        removeEventBtn.disableProperty().bind(eventsList.selectionModel.selectedItemProperty().isNull)
        addEvSourceBtn.disableProperty().bind(eventsList.selectionModel.selectedItemProperty().isNull)
        removeEvSourceBtn.disableProperty().bind(evSourcesList.selectionModel.selectedItemProperty().isNull)

        // PAGE editor wiring for selected source citation
        evSourcePageField.promptText = "PAGE"
        evSourcePageField.isDisable = true
        evSourcesList.selectionModel.selectedItemProperty().addListener { _, _, n ->
            evSourcePageField.isDisable = n == null
            evSourcePageField.text = n?.page ?: ""
        }
        evSourcePageField.textProperty().addListener { _, _, n ->
            val scSel = evSourcesList.selectionModel.selectedItem
            if (scSel != null) {
                scSel.page = n?.trim()
                evSourcesList.refresh()
            }
        }

        // Wire up note editor to update selected note's text
        noteList.selectionModel.selectedItemProperty().addListener { _, _, n ->
            noteEditor.text = n?.text ?: ""
            if (n != null) noteSourcesList.items.setAll(n.sources) else noteSourcesList.items.clear()
        }
        noteEditor.textProperty().addListener { _, _, n ->
            val sel = noteList.selectionModel.selectedItem
            if (sel != null) {
                sel.text = n
                noteList.refresh()
            }
        }

        // Remove buttons disabled when nothing selected
        removeTagBtn.disableProperty().bind(tagList.selectionModel.selectedItemProperty().isNull)
        removeNoteBtn.disableProperty().bind(noteList.selectionModel.selectedItemProperty().isNull)
        removeMediaBtn.disableProperty().bind(mediaList.selectionModel.selectedItemProperty().isNull)
        editMediaBtn.disableProperty().bind(mediaList.selectionModel.selectedItemProperty().isNull)
        openMediaBtn.disableProperty().bind(mediaList.selectionModel.selectedItemProperty().isNull)

        // Open link on double click if external; otherwise open edit dialog
        mediaList.setOnMouseClicked { evt ->
            if (evt.clickCount == 2) {
                val sel = mediaList.selectionModel.selectedItem ?: return@setOnMouseClicked
                val rel = sel.relativePath
                if (MediaManager.isExternalLink(rel)) {
                    val uri = MediaManager.toExternalUri(rel)
                    if (uri != null) {
                        try {
                            if (java.awt.Desktop.isDesktopSupported()) {
                                java.awt.Desktop.getDesktop().browse(uri)
                            }
                        } catch (ex: Exception) {
                            Dialogs.showError("Open Link", ex.message)
                        }
                    }
                } else {
                    val pf = projectFilePath ?: return@setOnMouseClicked
                    val dlg = MediaPropertiesDialog(pf, sel)
                    val res = dlg.showAndWait()
                    if (res.isPresent && java.lang.Boolean.TRUE == res.get()) {
                        mediaList.refresh()
                    }
                }
            }
        }
    }

    fun getView(): Node = scroller

    fun setProjectContext(data: ProjectRepository.ProjectData?, projectFilePath: Path?) {
        this.data = data
        this.projectFilePath = projectFilePath
        clearDetails()
    }

    fun setSelection(ids: Set<String>?) {
        if (ids == null || ids.isEmpty()) {
            selectionInfo.text = "(no selection)"
            currentId = null
            clearDetails()
            return
        }
        if (ids.size > 1) {
            selectionInfo.text = "Multiple selected (" + ids.size + ")"
            currentId = null
            clearDetails()
            return
        }
        currentId = ids.iterator().next()
        selectionInfo.text = "Selected: " + buildSelectionLabel(currentId)
        populateDetails()
    }

    private fun buildSelectionLabel(id: String?): String {
        if (data == null || id == null) return id ?: ""
        // Try individual first
        data!!.individuals.forEach { i ->
            if (id == i.id) {
                val first = i.firstName
                val last = i.lastName
                val full = ((first?.trim() ?: "") + " " + (last?.trim() ?: "")).trim()
                return if (full.isBlank()) id else full
            }
        }
        // Try family
        data!!.families.forEach { f ->
            if (id == f.id) {
                val husband = findIndividualById(f.husbandId)
                val wife = findIndividualById(f.wifeId)
                val surname = deriveFamilySurname(husband, wife)
                val initials = buildSpousesInitials(husband, wife)
                val label = when {
                    surname.isNotBlank() && initials.isNotBlank() -> "$surname — $initials"
                    surname.isNotBlank() -> surname
                    initials.isNotBlank() -> initials
                    else -> id
                }
                return label
            }
        }
        return id
    }

    private fun findIndividualById(indId: String?): Individual? {
        if (indId.isNullOrBlank() || data == null) return null
        return data!!.individuals.firstOrNull { indId == it.id }
    }

    private fun deriveFamilySurname(husband: Individual?, wife: Individual?): String {
        val h = husband?.lastName?.trim() ?: ""
        val w = wife?.lastName?.trim() ?: ""
        if (h.isNotBlank() && w.isNotBlank()) {
            if (h.equals(w, ignoreCase = true)) return h
            return h
        }
        if (h.isNotBlank()) return h
        if (w.isNotBlank()) return w
        return ""
    }

    private fun buildSpousesInitials(husband: Individual?, wife: Individual?): String {
        val hi = buildInitials(husband)
        val wi = buildInitials(wife)
        if (hi.isNotBlank() && wi.isNotBlank()) return "$hi и $wi"
        if (hi.isNotBlank()) return hi
        if (wi.isNotBlank()) return wi
        return ""
    }

    private fun buildInitials(person: Individual?): String {
        if (person == null) return ""
        val first = person.firstName
        if (!first.isNullOrBlank()) {
            val c = first.trim()[0].uppercaseChar()
            return "$c."
        }
        return ""
    }

    private fun populateDetails() {
        if (data == null || currentId == null) {
            clearDetails(); return
        }
        val ind = data!!.individuals.firstOrNull { it.id == currentId }
        val fam = data!!.families.firstOrNull { it.id == currentId }
        when {
            ind != null -> {
                tagList.items.setAll(ind.tags)
                noteList.items.setAll(ind.notes)
                mediaList.items.setAll(ind.media)
                eventsList.items.setAll(ind.events)
            }
            fam != null -> {
                tagList.items.setAll(fam.tags)
                noteList.items.setAll(fam.notes)
                mediaList.items.setAll(fam.media)
                eventsList.items.setAll(fam.events)
            }
            else -> clearDetails()
        }
        // Auto-select a meaningful event
        if (!eventsList.items.isEmpty()) {
            val sel = eventsList.selectionModel.selectedItem
            if (sel == null) {
                var prefer: GedcomEvent? = null
                eventsList.items.forEach { ev ->
                    val t = ev.type?.trim()?.uppercase(Locale.ROOT) ?: ""
                    if (t == "DEAT") { prefer = ev; return@forEach }
                    if (prefer == null && t == "BIRT") { prefer = ev }
                }
                if (prefer == null) prefer = eventsList.items[0]
                eventsList.selectionModel.select(prefer)
                evSourcesList.items.setAll(prefer!!.sources)
                evTypeField.text = prefer!!.type ?: ""
                evDateField.text = prefer!!.date ?: ""
                evPlaceField.text = prefer!!.place ?: ""
            }
        }
        // Expand/collapse sections
        try {
            val hasEvents = !eventsList.items.isEmpty()
            val hasTags = !tagList.items.isEmpty()
            val hasNotes = !noteList.items.isEmpty()
            val hasMedia = !mediaList.items.isEmpty()
            eventsPane.isExpanded = hasEvents
            tagsPane.isExpanded = !hasEvents && hasTags
            notesPane.isExpanded = !hasEvents && !hasTags && hasNotes
            mediaPane.isExpanded = !hasEvents && !hasTags && !hasNotes && hasMedia
        } catch (_: Throwable) { }

        mediaList.setCellFactory { MediaCell(projectFilePath) }
        tagList.setCellFactory { object : ListCell<Tag>() {
            override fun updateItem(item: Tag?, empty: Boolean) {
                super.updateItem(item, empty)
                text = if (empty || item == null) null else item.name
            }
        } }
        noteList.setCellFactory { object : ListCell<Note>() {
            override fun updateItem(item: Note?, empty: Boolean) {
                super.updateItem(item, empty)
                text = if (empty || item == null) null else preview(item.text)
            }
            private fun preview(s: String?): String = when {
                s == null -> ""
                s.length > 60 -> s.substring(0, 60) + "…"
                else -> s
            }
        } }
        eventsList.setCellFactory { object : ListCell<GedcomEvent>() {
            override fun updateItem(item: GedcomEvent?, empty: Boolean) {
                super.updateItem(item, empty)
                if (empty || item == null) { text = null } else {
                    val type = item.type ?: ""
                    val date = item.date ?: ""
                    val place = item.place ?: ""
                    val parts = mutableListOf<String>()
                    if (type.isNotBlank()) parts += type
                    if (date.isNotBlank()) parts += date
                    if (place.isNotBlank()) parts += place
                    text = parts.joinToString(" — ")
                }
            }
        } }
        evSourcesList.setCellFactory { object : ListCell<SourceCitation>() {
            override fun updateItem(item: SourceCitation?, empty: Boolean) {
                super.updateItem(item, empty)
                if (empty || item == null) { text = null } else {
                    val label = run {
                        val inlineText = item.text
                        if (!inlineText.isNullOrBlank()) inlineText.trim() else {
                            var title = "Unknown source"
                            val sid = item.sourceId
                            if (sid != null && data != null) {
                                data!!.sources.firstOrNull { it.id == sid }?.let { s ->
                                    title = if (!s.title.isNullOrBlank()) s.title!! else title
                                }
                            }
                            title
                        }
                    }
                    val page = item.page ?: ""
                    text = label + if (page.isBlank()) "" else " — PAGE: $page"
                }
            }
        } }
    }

    private fun clearDetails() {
        tagList.items.clear()
        noteList.items.clear()
        noteEditor.clear()
        noteSourcesList.items.clear()
        noteSourcePageField.clear()
        mediaList.items.clear()
        eventsList.items.clear()
        evSourcesList.items.clear()
        evTypeField.clear()
        evDateField.clear()
        evPlaceField.clear()
    }

    private fun buildTagsPane(): Pane {
        val box = VBox(6.0)
        tagInput.promptText = "New tag..."
        val actions = HBox(6.0, tagInput, addTagBtn, removeTagBtn)
        tagList.prefHeight = 100.0
        VBox.setVgrow(tagList, Priority.NEVER)
        box.children.addAll(tagList, actions)

        addTagBtn.setOnAction {
            val d = data ?: return@setOnAction
            val id = currentId ?: return@setOnAction
            val name = tagInput.text
            if (name.isNullOrBlank()) return@setOnAction
            try {
                val tag = Tag().apply { this.name = name.trim() }
                val ts = TagService(d)
                if (isIndividual(id)) {
                    ts.assignTagToIndividual(id, tag)
                } else if (isFamily(id)) {
                    ts.assignTagToFamily(id, tag)
                }
                tagInput.clear()
                populateDetails()
            } catch (ex: Exception) {
                Dialogs.showError("Add Tag Failed", ex.message)
            }
        }

        removeTagBtn.setOnAction {
            val d = data ?: return@setOnAction
            val id = currentId ?: return@setOnAction
            val sel = tagList.selectionModel.selectedItem ?: return@setOnAction
            val ts = TagService(d)
            if (isIndividual(id)) ts.removeTagFromIndividual(id, sel.id)
            else if (isFamily(id)) ts.removeTagFromFamily(id, sel.id)
            populateDetails()
        }

        return box
    }

    private fun buildNotesPane(): Pane {
        val box = VBox(6.0)
        noteEditor.promptText = "Note text..."
        noteEditor.isWrapText = true
        noteEditor.prefRowCount = 8
        noteEditor.minHeight = 120.0
        val actions = HBox(6.0, addNoteBtn, removeNoteBtn)
        noteList.prefHeight = 100.0
        VBox.setVgrow(noteList, Priority.NEVER)
        VBox.setVgrow(noteEditor, Priority.ALWAYS)

        // Note sources UI
        val lNoteSrc = Label("Sources:")
        addNoteSourceBtn.tooltip = Tooltip("Add Source…")
        manageNoteSourcesBtn.tooltip = Tooltip("Manage Sources…")
        removeNoteSourceBtn.tooltip = Tooltip("Remove citation")
        val noteSrcActions = HBox(6.0, addNoteSourceBtn, manageNoteSourcesBtn, removeNoteSourceBtn)
        val noteSrcBox = VBox(4.0, lNoteSrc, noteSrcActions, noteSourcesList, Label("PAGE:"), noteSourcePageField)
        noteSourcesList.prefHeight = 80.0
        VBox.setVgrow(noteSourcesList, Priority.NEVER)

        addNoteSourceBtn.disableProperty().bind(noteList.selectionModel.selectedItemProperty().isNull)
        removeNoteSourceBtn.disableProperty().bind(noteSourcesList.selectionModel.selectedItemProperty().isNull)
        noteSourcePageField.promptText = "PAGE"
        noteSourcePageField.isDisable = true
        noteSourcesList.selectionModel.selectedItemProperty().addListener { _, _, n ->
            noteSourcePageField.isDisable = n == null
            noteSourcePageField.text = n?.page ?: ""
        }
        noteSourcePageField.textProperty().addListener { _, _, n ->
            val scSel = noteSourcesList.selectionModel.selectedItem
            if (scSel != null) {
                scSel.page = n?.trim()
                noteSourcesList.refresh()
            }
        }

        addNoteBtn.setOnAction {
            if (data == null || currentId == null) return@setOnAction
            val note = Note().apply { text = "New note" }
            if (isIndividual(currentId)) {
                data!!.individuals.firstOrNull { it.id == currentId }?.notes?.add(note)
            } else if (isFamily(currentId)) {
                data!!.families.firstOrNull { it.id == currentId }?.notes?.add(note)
            }
            populateDetails()
            if (!noteList.items.isEmpty()) {
                val lastIdx = noteList.items.size - 1
                noteList.selectionModel.select(lastIdx)
                noteList.scrollTo(lastIdx)
                noteEditor.requestFocus()
                noteEditor.selectAll()
            }
        }

        removeNoteBtn.setOnAction {
            if (data == null || currentId == null) return@setOnAction
            val sel = noteList.selectionModel.selectedItem ?: return@setOnAction
            if (isIndividual(currentId)) {
                data!!.individuals.firstOrNull { it.id == currentId }?.notes?.removeIf { it.id == sel.id }
            } else if (isFamily(currentId)) {
                data!!.families.firstOrNull { it.id == currentId }?.notes?.removeIf { it.id == sel.id }
            }
            noteEditor.clear()
            populateDetails()
        }

        addNoteSourceBtn.setOnAction {
            val note = noteList.selectionModel.selectedItem
            if (note == null) {
                Dialogs.showError("Add Source", "Сначала выберите заметку в списке.")
                return@setOnAction
            }
            val d = data ?: return@setOnAction
            if (d.sources.isEmpty()) {
                Dialogs.showError("Add Source", "В проекте нет записей источников (SOUR). Добавьте источник в проект, затем повторите попытку.")
                return@setOnAction
            }
            val labels = ArrayList<String>()
            for (i in d.sources.indices) {
                val s = d.sources[i]
                val t = if (!s.title.isNullOrBlank()) s.title!! else s.id
                labels.add("${i + 1}. $t")
            }
            val dlg = ChoiceDialog(labels[0], labels)
            dlg.title = "Attach Source"
            dlg.headerText = "Select a Source to cite"
            dlg.contentText = "Source:"
            val srcSel = dlg.showAndWait()
            if (srcSel.isEmpty) return@setOnAction
            val idx = labels.indexOf(srcSel.get())
            if (idx < 0) return@setOnAction
            val chosen = d.sources[idx]
            val pageDlg = TextInputDialog("")
            pageDlg.title = "Source Page"
            pageDlg.headerText = "Optional: specify PAGE or details"
            pageDlg.contentText = "PAGE:"
            val pageRes = pageDlg.showAndWait()
            val sc = SourceCitation()
            sc.sourceId = chosen.id
            if (pageRes.isPresent) sc.page = pageRes.get().trim()
            note.sources.add(sc)
            noteSourcesList.items.setAll(note.sources)
        }
        removeNoteSourceBtn.setOnAction {
            val note = noteList.selectionModel.selectedItem
            val sc = noteSourcesList.selectionModel.selectedItem
            if (note == null || sc == null) return@setOnAction
            note.sources.removeIf { it.id == sc.id }
            noteSourcesList.items.setAll(note.sources)
        }
        manageNoteSourcesBtn.setOnAction {
            val d = data ?: return@setOnAction
            try {
                SourcesManagerDialog(d).showAndWait()
                noteSourcesList.refresh()
            } catch (ex: Throwable) {
                Dialogs.showError("Sources", ex.message)
            }
        }

        noteSourcesList.setCellFactory { object : ListCell<SourceCitation>() {
            override fun updateItem(item: SourceCitation?, empty: Boolean) {
                super.updateItem(item, empty)
                if (empty || item == null) { text = null } else {
                    val label = run {
                        val inlineText = item.text
                        if (!inlineText.isNullOrBlank()) inlineText.trim() else {
                            var title = "Unknown source"
                            val sid = item.sourceId
                            if (sid != null && data != null) {
                                data!!.sources.firstOrNull { it.id == sid }?.let { s ->
                                    title = if (!s.title.isNullOrBlank()) s.title!! else title
                                }
                            }
                            title
                        }
                    }
                    val page = item.page ?: ""
                    text = label + if (page.isBlank()) "" else " — PAGE: $page"
                }
            }
        } }

        box.children.addAll(noteList, noteEditor, actions, noteSrcBox)
        return box
    }

    private fun buildMediaPane(): Pane {
        val box = VBox(6.0)
        val actions = HBox(6.0, addMediaBtn, editMediaBtn, openMediaBtn, removeMediaBtn)
        mediaList.prefHeight = 150.0
        VBox.setVgrow(mediaList, Priority.NEVER)
        box.children.addAll(mediaList, actions)

        addMediaBtn.setOnAction {
            if (data == null || currentId == null) return@setOnAction
            val chooser = javafx.stage.FileChooser()
            chooser.title = "Add Media"
            chooser.extensionFilters.addAll(
                javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                javafx.stage.FileChooser.ExtensionFilter("All Files", "*.*")
            )
            val file = chooser.showOpenDialog(activeWindow)
            if (file == null) return@setOnAction
            try {
                val pf = projectFilePath ?: return@setOnAction
                val d = data ?: return@setOnAction
                val id = currentId ?: return@setOnAction
                val att = MediaManager.copyIntoProject(pf, file.toPath())
                val ms = MediaService(d)
                if (isIndividual(id)) ms.attachToIndividual(id, att)
                else if (isFamily(id)) ms.attachToFamily(id, att)
                populateDetails()
            } catch (ex: IOException) {
                Dialogs.showError("Add Media Failed", ex.message)
            }
        }

        editMediaBtn.setOnAction {
            val sel = mediaList.selectionModel.selectedItem ?: return@setOnAction
            val pf = projectFilePath ?: return@setOnAction
            val dlg = MediaPropertiesDialog(pf, sel)
            val res = dlg.showAndWait()
            if (res.isPresent && java.lang.Boolean.TRUE == res.get()) {
                mediaList.refresh()
            }
        }

        openMediaBtn.setOnAction {
            val sel = mediaList.selectionModel.selectedItem ?: return@setOnAction
            val rel = sel.relativePath
            if (MediaManager.isExternalLink(rel)) {
                val uri = MediaManager.toExternalUri(rel)
                if (uri == null) {
                    Dialogs.showError("Open Link", "Неверная ссылка: $rel")
                    return@setOnAction
                }
                try {
                    if (java.awt.Desktop.isDesktopSupported()) {
                        java.awt.Desktop.getDesktop().browse(uri)
                    } else {
                        Dialogs.showError("Open Link", "Desktop API не поддерживается на этой платформе")
                    }
                } catch (ex: Exception) {
                    Dialogs.showError("Open Link", ex.message)
                }
            } else {
                try {
                    val pf = projectFilePath ?: return@setOnAction
                    val p = MediaManager.resolveAttachmentPath(pf, sel)
                    if (p != null && Files.exists(p)) {
                        if (java.awt.Desktop.isDesktopSupported()) {
                            java.awt.Desktop.getDesktop().open(p.toFile())
                        }
                    } else {
                        Dialogs.showError("Open Media", "Файл не найден")
                    }
                } catch (ex: Exception) {
                    Dialogs.showError("Open Media", ex.message)
                }
            }
        }

        removeMediaBtn.setOnAction {
            val d = data ?: return@setOnAction
            val id = currentId ?: return@setOnAction
            val sel = mediaList.selectionModel.selectedItem ?: return@setOnAction
            val ms = MediaService(d)
            if (isIndividual(id)) ms.detachFromIndividual(id, sel.id)
            else if (isFamily(id)) ms.detachFromFamily(id, sel.id)
            populateDetails()
        }

        return box
    }

    private fun buildEventsPane(): Pane {
        val box = VBox(8.0)
        val evActions = HBox(6.0, addEventBtn, removeEventBtn)
        eventsList.prefHeight = 100.0
        VBox.setVgrow(eventsList, Priority.NEVER)
        val fieldsBox = VBox(6.0)
        evTypeField.promptText = "Type (e.g., BIRT, DEAT, MARR)"
        evDateField.promptText = "Date (e.g., 5 JAN 1980, ABT 1900)"
        evPlaceField.promptText = "Place"
        val evDatePickerBtn = Button("...")
        val hbEvDate = HBox(6.0, evDateField, evDatePickerBtn)
        evDatePickerBtn.setOnAction {
            val dateDlg = DatePhraseDialog()
            val initial = evDateField.text
            val res = dateDlg.show(initial)
            if (res.isPresent) {
                val value = res.get()
                evDateField.text = value
                val selEv = eventsList.selectionModel.selectedItem
                if (selEv != null) {
                    selEv.date = value
                    eventsList.refresh()
                }
            }
        }
        fieldsBox.children.addAll(Label("Type:"), evTypeField, Label("Date:"), hbEvDate, Label("Place:"), evPlaceField)

        val lSrc = Label("Sources:")
        addEvSourceBtn.tooltip = Tooltip("Add Source…")
        manageSourcesBtn.tooltip = Tooltip("Manage Sources…")
        removeEvSourceBtn.tooltip = Tooltip("Remove citation")
        val srcActions = HBox(6.0, addEvSourceBtn, manageSourcesBtn, removeEvSourceBtn)
        val evSrcBox = VBox(4.0, lSrc, srcActions, evSourcesList, Label("PAGE:"), evSourcePageField)
        evSourcesList.prefHeight = 80.0
        VBox.setVgrow(evSourcesList, Priority.NEVER)

        box.children.addAll(eventsList, evActions, fieldsBox, evSrcBox)

        addEventBtn.setOnAction {
            if (data == null || currentId == null) return@setOnAction
            val types = javafx.collections.FXCollections.observableArrayList("BIRT", "DEAT", "BURI", "MARR", "ADOP", "RESI", "EVEN")
            val dlg = ChoiceDialog("EVEN", types)
            dlg.title = "Add Event"
            dlg.headerText = "Select GEDCOM event type"
            val sel = dlg.showAndWait()
            if (sel.isEmpty) return@setOnAction
            val ev = GedcomEvent().apply { type = sel.get() }
            if (isIndividual(currentId)) {
                data!!.individuals.firstOrNull { it.id == currentId }?.events?.add(ev)
            } else if (isFamily(currentId)) {
                data!!.families.firstOrNull { it.id == currentId }?.events?.add(ev)
            }
            populateDetails()
            eventsList.selectionModel.select(ev)
            eventsList.scrollTo(ev)
            evTypeField.requestFocus()
        }
        removeEventBtn.setOnAction {
            if (data == null || currentId == null) return@setOnAction
            val sel = eventsList.selectionModel.selectedItem ?: return@setOnAction
            if (isIndividual(currentId)) {
                data!!.individuals.firstOrNull { it.id == currentId }?.events?.removeIf { it.id == sel.id }
            } else if (isFamily(currentId)) {
                data!!.families.firstOrNull { it.id == currentId }?.events?.removeIf { it.id == sel.id }
            }
            populateDetails()
        }
        addEvSourceBtn.setOnAction {
            val ev = eventsList.selectionModel.selectedItem
            if (ev == null) {
                Dialogs.showError("Add Source", "Сначала выберите событие в списке.")
                return@setOnAction
            }
            val d = data ?: return@setOnAction
            if (d.sources.isEmpty()) {
                Dialogs.showError("Add Source", "В проекте нет записей источников (SOUR). Добавьте источник в проект, затем повторите попытку.")
                return@setOnAction
            }
            val labels = ArrayList<String>()
            for (i in d.sources.indices) {
                val s = d.sources[i]
                val t = if (!s.title.isNullOrBlank()) s.title!! else s.id
                labels.add("${i + 1}. $t")
            }
            val dlg = ChoiceDialog(labels[0], labels)
            dlg.title = "Attach Source"
            dlg.headerText = "Select a Source to cite"
            dlg.contentText = "Source:"
            val srcSel = dlg.showAndWait()
            if (srcSel.isEmpty) return@setOnAction
            val idx = labels.indexOf(srcSel.get())
            if (idx < 0) return@setOnAction
            val chosen = d.sources[idx]
            val pageDlg = TextInputDialog("")
            pageDlg.title = "Source Page"
            pageDlg.headerText = "Optional: specify PAGE or details"
            pageDlg.contentText = "PAGE:"
            val pageRes = pageDlg.showAndWait()
            val sc = SourceCitation()
            sc.sourceId = chosen.id
            if (pageRes.isPresent) sc.page = pageRes.get().trim()
            ev.sources.add(sc)
            evSourcesList.items.setAll(ev.sources)
        }
        removeEvSourceBtn.setOnAction {
            val ev = eventsList.selectionModel.selectedItem
            val sc = evSourcesList.selectionModel.selectedItem
            if (ev == null || sc == null) return@setOnAction
            ev.sources.removeIf { it.id == sc.id }
            evSourcesList.items.setAll(ev.sources)
        }
        manageSourcesBtn.setOnAction {
            val d = data ?: return@setOnAction
            try {
                SourcesManagerDialog(d).showAndWait()
                evSourcesList.refresh()
            } catch (ex: Throwable) {
                Dialogs.showError("Sources", ex.message)
            }
        }

        return box
    }

    private val activeWindow: javafx.stage.Stage?
        get() {
            for (w in javafx.stage.Window.getWindows()) {
                if (w is javafx.stage.Stage && w.isShowing) return w
            }
            return null
        }

    private fun isIndividual(id: String?): Boolean = data != null && data!!.individuals.any { it.id == id }
    private fun isFamily(id: String?): Boolean = data != null && data!!.families.any { it.id == id }

    private class MediaCell(private val projectFilePath: Path?) : ListCell<MediaAttachment>() {
        private val root = HBox(8.0)
        private val iv = ImageView().apply {
            fitWidth = 64.0
            fitHeight = 48.0
            isPreserveRatio = true
        }
        private val name = Label()

        init {
            root.children.addAll(iv, name)
        }

        override fun updateItem(item: MediaAttachment?, empty: Boolean) {
            super.updateItem(item, empty)
            if (empty || item == null) { graphic = null; return }
            var display = if (!item.fileName.isNullOrBlank()) item.fileName!! else item.relativePath ?: "(media)"
            iv.image = null
            val rel = item.relativePath
            val isLink = MediaManager.isExternalLink(rel)
            if (isLink) {
                name.text = "$display (link)"
                graphic = root
                return
            }
            var missing = false
            try {
                val p = MediaManager.resolveAttachmentPath(projectFilePath, item)
                if (p != null && Files.exists(p)) {
                    val lower = p.fileName.toString().lowercase()
                    if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif")) {
                        val img: Image = Image(p.toUri().toString(), 128.0, 128.0, true, true, true)
                        iv.image = img
                    }
                } else missing = true
            } catch (ex: IOException) {
                missing = true
            }
            if (missing) display += " (missing)"
            name.text = display
            graphic = root
        }
    }
}
