package com.family.tree.ui.components.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.family.tree.core.ProjectData
import com.family.tree.core.model.FamilyId
import com.family.tree.core.model.Individual
import com.family.tree.core.model.IndividualId
import com.family.tree.core.search.QuickSearchService

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LeftSidebar(
    project: ProjectData,
    searchService: QuickSearchService,
    selectedIds: Set<IndividualId>,
    onSelectIndividual: (Individual) -> Unit,
    onSelectFamily: (FamilyId, Set<IndividualId>) -> Unit,
    onEditIndividual: (IndividualId) -> Unit,
    onEditFamily: (FamilyId) -> Unit,
    modifier: Modifier = Modifier
) {
    var individualsSearchQuery by remember { mutableStateOf("") }
    var familiesSearchQuery by remember { mutableStateOf("") }

    Box(
        modifier = modifier.fillMaxHeight().background(Color(0x0D000000)),
        contentAlignment = Alignment.TopStart
    ) {
        Column(Modifier.fillMaxSize()) {
            var leftTab by remember { mutableStateOf(0) }
            @Suppress("DEPRECATION")
            TabRow(selectedTabIndex = leftTab) {
                Tab(
                    selected = leftTab == 0,
                    onClick = { leftTab = 0 },
                    text = { Text("Individuals") }
                )
                Tab(
                    selected = leftTab == 1,
                    onClick = { leftTab = 1 },
                    text = { Text("Families") }
                )
            }
            when (leftTab) {
                0 -> {
                    Column(Modifier.fillMaxSize().padding(12.dp)) {
                        OutlinedTextField(
                            value = individualsSearchQuery,
                            onValueChange = { individualsSearchQuery = it },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            placeholder = { Text("Filter by name or tag...") },
                            singleLine = true
                        )

                        val filteredIndividuals = remember(individualsSearchQuery, project.individuals) {
                            if (individualsSearchQuery.isBlank()) {
                                project.individuals
                            } else {
                                searchService.findIndividualsByName(individualsSearchQuery)
                            }
                        }

                        LazyColumn(Modifier.weight(1f)) {
                            items(filteredIndividuals) { ind ->
                                val isSelected = selectedIds.contains(ind.id)
                                val bg = if (isSelected) Color(0x201976D2) else Color.Unspecified
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(bg)
                                        .combinedClickable(
                                            onClick = { onSelectIndividual(ind) },
                                            onDoubleClick = { onEditIndividual(ind.id) }
                                        )
                                        .padding(vertical = 4.dp, horizontal = 6.dp)
                                ) {
                                    Text(
                                        "• ${ind.displayName}",
                                        color = if (isSelected) Color(0xFF0D47A1) else Color.Unspecified
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    Column(Modifier.fillMaxSize().padding(12.dp)) {
                        fun formatSpouseName(indId: IndividualId?): String {
                            if (indId == null) return ""
                            val ind = project.individuals.find { it.id == indId } ?: return indId.value
                            val lastName = ind.lastName.trim()
                            val firstName = ind.firstName.trim()
                            val sb = StringBuilder()
                            if (lastName.isNotEmpty()) sb.append(lastName)
                            if (firstName.isNotEmpty()) {
                                if (sb.isNotEmpty()) sb.append(' ')
                                sb.append(firstName.first().uppercaseChar()).append('.')
                            }
                            val result = sb.toString()
                            return if (result.isBlank()) indId.value else result
                        }

                        OutlinedTextField(
                            value = familiesSearchQuery,
                            onValueChange = { familiesSearchQuery = it },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            placeholder = { Text("Filter by name or tag...") },
                            singleLine = true
                        )

                        val filteredFamilies = remember(familiesSearchQuery, project.families, project.individuals) {
                            if (familiesSearchQuery.isBlank()) {
                                project.families
                            } else {
                                searchService.findFamiliesBySpouseName(familiesSearchQuery)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Husband",
                                modifier = Modifier.weight(1f),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "Wife",
                                modifier = Modifier.weight(1f),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "Child",
                                modifier = Modifier.width(40.dp),
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }

                        LazyColumn(Modifier.weight(1f)) {
                            items(filteredFamilies) { fam ->
                                val husbandName = formatSpouseName(fam.husbandId)
                                val wifeName = formatSpouseName(fam.wifeId)
                                val childrenCount = fam.childrenIds.size.toString()

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                val familyMemberIds = mutableSetOf<IndividualId>()
                                                fam.husbandId?.let { familyMemberIds.add(it) }
                                                fam.wifeId?.let { familyMemberIds.add(it) }
                                                familyMemberIds.addAll(fam.childrenIds)
                                                onSelectFamily(fam.id, familyMemberIds)
                                            },
                                            onDoubleClick = { onEditFamily(fam.id) }
                                        )
                                        .padding(vertical = 4.dp, horizontal = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = husbandName,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = wifeName,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = childrenCount,
                                        modifier = Modifier.width(40.dp),
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
