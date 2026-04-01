package com.family.tree.ui.components.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.family.tree.core.ProjectData
import com.family.tree.core.model.FamilyId
import com.family.tree.core.model.IndividualId
import com.family.tree.ui.PropertiesInspector

@Composable
fun RightSidebar(
    project: ProjectData,
    selectedIds: Set<IndividualId>,
    selectedFamilyId: FamilyId?,
    onUpdateProject: (ProjectData) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxHeight().background(Color(0x0D000000)),
        contentAlignment = Alignment.TopStart
    ) {
        val selectedFamily = selectedFamilyId?.let { id -> project.families.find { it.id == id } }
        if (selectedFamily != null) {
            PropertiesInspector(
                family = selectedFamily,
                project = project,
                onUpdateFamily = { updated ->
                    val idx = project.families.indexOfFirst { it.id == updated.id }
                    if (idx >= 0) {
                        onUpdateProject(
                            project.copy(
                                families = project.families.toMutableList().also { it[idx] = updated }
                            )
                        )
                    }
                }
            )
        } else {
            val selected = selectedIds.firstOrNull()?.let { id -> project.individuals.find { it.id == id } }
            if (selected == null) {
                Column(Modifier.padding(12.dp).fillMaxWidth()) {
                    Text("Properties\nSelect a person or family…")
                }
            } else {
                PropertiesInspector(
                    individual = selected,
                    project = project,
                    onUpdateIndividual = { updated ->
                        val idx = project.individuals.indexOfFirst { it.id == updated.id }
                        if (idx >= 0) {
                            onUpdateProject(
                                project.copy(
                                    individuals = project.individuals.toMutableList().also { it[idx] = updated }
                                )
                            )
                        }
                    }
                )
            }
        }
    }
}
