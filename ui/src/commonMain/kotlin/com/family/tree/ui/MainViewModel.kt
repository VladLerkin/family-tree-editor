package com.family.tree.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family.tree.core.ProjectData
import com.family.tree.core.io.LoadedProject
import com.family.tree.core.layout.ProjectLayout
import com.family.tree.core.layout.SimpleTreeLayout
import com.family.tree.core.model.FamilyId
import com.family.tree.core.model.Individual
import com.family.tree.core.model.IndividualId
import com.family.tree.core.sample.SampleData
import com.family.tree.core.geometry.Vec2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainState(
    val project: ProjectData,
    val projectLayout: ProjectLayout? = null,
    val cachedPositions: Map<IndividualId, Vec2> = emptyMap(),
    val selectedIds: Set<IndividualId> = emptySet(),
    val selectedFamilyId: FamilyId? = null,
    val shouldAutoFit: Boolean = false,
    val activeDialog: AppDialog? = null
)

sealed interface AppDialog {
    data class EditPerson(val id: IndividualId) : AppDialog
    data class EditFamily(val id: FamilyId) : AppDialog
    data object SourcesManager : AppDialog
    data object About : AppDialog
    data object AiSettings : AppDialog
    data object VoiceInput : AppDialog
    
    // AI Progress and Info
    data class AiProgress(val message: String) : AppDialog
    data class AiInfo(val message: String, val isPermissionError: Boolean = false) : AppDialog
}

class MainViewModel : ViewModel() {

    private val _state = MutableStateFlow(
        MainState(
            project = ProjectData(emptyList(), emptyList(), emptyList())
        )
    )
    val state: StateFlow<MainState> = _state.asStateFlow()

    init {
        loadSampleData()
    }

    private fun loadSampleData() = viewModelScope.launch(Dispatchers.Default) {
        val loadedSample = SampleData.simpleThreeGenWithLayout()
        val positions = SimpleTreeLayout.layout(loadedSample.data.individuals, loadedSample.data.families)
        _state.update { 
            it.copy(
                project = loadedSample.data,
                projectLayout = loadedSample.layout,
                cachedPositions = positions
            )
        }
    }

    fun newProject() = viewModelScope.launch(Dispatchers.Default) {
        _state.update {
            it.copy(
                project = ProjectData(emptyList(), emptyList(), emptyList()),
                projectLayout = null,
                cachedPositions = emptyMap(),
                selectedIds = emptySet(),
                selectedFamilyId = null
            )
        }
    }

    fun loadProject(loadedProject: LoadedProject, autoFit: Boolean = false) = viewModelScope.launch(Dispatchers.Default) {
        val positions = SimpleTreeLayout.layout(loadedProject.data.individuals, loadedProject.data.families)
        _state.update {
            it.copy(
                project = loadedProject.data,
                projectLayout = loadedProject.layout,
                cachedPositions = positions,
                selectedIds = emptySet(),
                selectedFamilyId = null,
                shouldAutoFit = autoFit
            )
        }
    }

    fun selectIndividual(id: IndividualId) {
        _state.update { it.copy(selectedIds = setOf(id), selectedFamilyId = null) }
    }

    fun selectFamily(familyId: FamilyId, memberIds: Set<IndividualId>) {
        _state.update { it.copy(selectedIds = memberIds, selectedFamilyId = familyId) }
    }

    fun clearSelection() {
        _state.update { it.copy(selectedIds = emptySet(), selectedFamilyId = null) }
    }

    fun markAutoFitConsumed() {
        _state.update { it.copy(shouldAutoFit = false) }
    }

    fun addIndividual(individual: Individual) = viewModelScope.launch(Dispatchers.Default) {
        val newIndividuals = _state.value.project.individuals + individual
        val newProject = _state.value.project.copy(individuals = newIndividuals)
        val positions = SimpleTreeLayout.layout(newIndividuals, newProject.families)
        _state.update {
            it.copy(
                project = newProject,
                cachedPositions = positions,
                selectedIds = setOf(individual.id),
                selectedFamilyId = null
            )
        }
    }

    fun deleteIndividual(idToDelete: IndividualId) = viewModelScope.launch(Dispatchers.Default) {
        val currentProject = _state.value.project
        val newIndividuals = currentProject.individuals.filter { it.id != idToDelete }
        val newFamilies = currentProject.families.map { family ->
            family.copy(
                husbandId = if (family.husbandId == idToDelete) null else family.husbandId,
                wifeId = if (family.wifeId == idToDelete) null else family.wifeId,
                childrenIds = family.childrenIds.filter { it != idToDelete }
            )
        }
        val newProject = currentProject.copy(individuals = newIndividuals, families = newFamilies)
        val positions = SimpleTreeLayout.layout(newIndividuals, newFamilies)
        
        _state.update {
            it.copy(
                project = newProject,
                cachedPositions = positions,
                selectedIds = emptySet(),
                selectedFamilyId = null
            )
        }
    }

    fun updateProjectInfo(project: ProjectData) = viewModelScope.launch(Dispatchers.Default) {
        val positions = SimpleTreeLayout.layout(project.individuals, project.families)
        _state.update { 
            it.copy(
                project = project,
                cachedPositions = positions
            )
        }
    }

    fun openDialog(dialog: AppDialog) {
        _state.update { it.copy(activeDialog = dialog) }
    }

    fun closeDialog() {
        _state.update { it.copy(activeDialog = null) }
    }
}
