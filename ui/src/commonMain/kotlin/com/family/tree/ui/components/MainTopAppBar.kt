package com.family.tree.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.family.tree.ui.AppActions
import com.family.tree.ui.ExitAppAction
import com.family.tree.ui.PlatformEnv

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    scale: Float,
    modifier: Modifier = Modifier
) {
    if (!PlatformEnv.isDesktop) {
        var showMenu by remember { mutableStateOf(false) }
        var showImportMenu by remember { mutableStateOf(false) }
        var shouldExit by remember { mutableStateOf(false) }
        
        TopAppBar(
            title = { Text("Family Tree Editor") },
            actions = {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More"
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(text = { Text("New") }, onClick = { showMenu = false; AppActions.newProject() })
                    DropdownMenuItem(text = { Text("Open") }, onClick = { showMenu = false; AppActions.openPed() })
                    DropdownMenuItem(text = { Text("Save") }, onClick = { showMenu = false; AppActions.savePed() })
                    DropdownMenuItem(text = { Text("Import...") }, onClick = { showMenu = false; showImportMenu = true })
                    DropdownMenuItem(text = { Text("Import AI Text") }, onClick = { showMenu = false; AppActions.importAiText() })
                    DropdownMenuItem(text = { Text("Voice Input 🎤") }, onClick = { showMenu = false; AppActions.voiceInput() })
                    DropdownMenuItem(text = { Text("Export GEDCOM") }, onClick = { showMenu = false; AppActions.exportGedcom() })
                    DropdownMenuItem(text = { Text("Export Markdown Tree") }, onClick = { showMenu = false; AppActions.exportMarkdownTree() })
                    DropdownMenuItem(text = { Text("Export SVG (Current)") }, onClick = { showMenu = false; AppActions.exportSvgCurrent() })
                    DropdownMenuItem(text = { Text("Export SVG (Fit)") }, onClick = { showMenu = false; AppActions.exportSvgFit() })
                    DropdownMenuItem(text = { Text("Manage Sources...") }, onClick = { showMenu = false; AppActions.manageSources() })
                    DropdownMenuItem(text = { Text("AI Settings...") }, onClick = { showMenu = false; AppActions.showAiSettings() })
                    DropdownMenuItem(text = { Text("Autoresearch Agent...") }, onClick = { showMenu = false; AppActions.showAutoSearch() })
                    DropdownMenuItem(text = { Text("About") }, onClick = { showMenu = false; AppActions.showAbout() })
                    DropdownMenuItem(text = { Text("Exit") }, onClick = { showMenu = false; shouldExit = true })
                }
                DropdownMenu(
                    expanded = showImportMenu,
                    onDismissRequest = { showImportMenu = false }
                ) {
                    DropdownMenuItem(text = { Text(".rel") }, onClick = { showImportMenu = false; AppActions.importRel() })
                    DropdownMenuItem(text = { Text("GEDCOM") }, onClick = { showImportMenu = false; AppActions.importGedcom() })
                }
            }
        )
        if (shouldExit) {
            ExitAppAction(onExit = { AppActions.exit() })
        }
    } else {
        // Desktop: Simple toolbar with title and zoom (menu is in native MenuBar)
        Row(
            modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Family Tree Editor", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Text("${(scale * 100).toInt()}%", modifier = Modifier.width(56.dp), textAlign = TextAlign.Center)
        }
    }
}
