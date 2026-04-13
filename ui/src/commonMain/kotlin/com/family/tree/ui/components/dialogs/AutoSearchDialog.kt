package com.family.tree.ui.components.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.family.tree.core.ProjectData
import com.family.tree.core.ai.agent.AgentProposal
import com.family.tree.core.ai.agent.AgentService
import com.family.tree.core.ai.agent.PromptTemplate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoSearchDialog(
    projectData: ProjectData,
    agentService: AgentService,
    onDismissRequest: () -> Unit
) {
    var prompts by remember { mutableStateOf<List<PromptTemplate>>(emptyList()) }
    var isLoadingPrompts by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        try {
            prompts = agentService.loadAvailablePrompts()
        } catch (e: Exception) {
            println("[DEBUG_LOG] AutoSearchDialog: ERROR loading prompts: ${e.message}")
            e.printStackTrace()
        } finally {
            isLoadingPrompts = false
        }
    }
    
    var selectedPrompt by remember { mutableStateOf<PromptTemplate?>(null) }
    var isRunning by remember { mutableStateOf(false) }
    var finalProposal by remember { mutableStateOf<AgentProposal?>(null) }
    
    val logs by agentService.agentLogs.collectAsState()
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = { if (!isRunning) onDismissRequest() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AI Autoresearch Hub", style = MaterialTheme.typography.headlineMedium)
                    IconButton(onClick = onDismissRequest, enabled = !isRunning) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (finalProposal != null) {
                    // Phase 3: Result
                    Text("Research Proposal", style = MaterialTheme.typography.titleLarge)
                    Text("The agent has completed the research and suggests these updates:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = MaterialTheme.shapes.medium)
                            .padding(16.dp)
                    ) {
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                item {
                                    Text(
                                        text = finalProposal!!.results,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(onClick = { finalProposal = null }) {
                            Text("Back to Prompts")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { onDismissRequest() }) {
                            Text("Finish")
                        }
                    }
                } else if (isRunning || logs.isNotEmpty()) {
                    // Phase 2: Researching / Progress
                    Text("Research in Progress", style = MaterialTheme.typography.titleLarge)
                    Text("Current Task: ${selectedPrompt?.name}", style = MaterialTheme.typography.bodyMedium)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isRunning) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("The AI agent is consulting methodology guides and searching the web...", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text("Research completed.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Agent Output Console:", style = MaterialTheme.typography.labelMedium)
                    
                    val listState = rememberLazyListState()
                    LaunchedEffect(logs.size) {
                        if (logs.isNotEmpty()) {
                            listState.animateScrollToItem(logs.size - 1)
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium)
                            .padding(8.dp)
                    ) {
                        LazyColumn(state = listState) {
                            items(logs) { logMsg ->
                                Text(
                                    text = logMsg,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                    
                    if (!isRunning) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { finalProposal = null; agentService.clearLogs() }, modifier = Modifier.align(Alignment.End)) {
                            Text("Run Another Prompt")
                        }
                    }
                } else {
                    // Phase 1: Selector
                    Text("Select a Research Methodology", style = MaterialTheme.typography.titleLarge)
                    Text("Choose a specialized AI agent to audit or expand your family tree:", style = MaterialTheme.typography.bodyMedium)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isLoadingPrompts) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (prompts.isEmpty()) {
                        val config = remember { agentService.loadConfig() }
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No methodology prompts found.", color = MaterialTheme.colorScheme.error)
                            Text("Path: ${config.autoresearchRepoPath}", style = MaterialTheme.typography.bodySmall)
                            Button(onClick = { /* Could open settings */ }, modifier = Modifier.padding(top = 8.dp)) {
                                Text("Check AI Settings")
                            }
                        }
                    }

                    // Scrollable List of Prompts
                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(prompts) { prompt ->
                                Card(
                                    onClick = { selectedPrompt = prompt },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selectedPrompt == prompt) 
                                            MaterialTheme.colorScheme.primaryContainer 
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    border = if (selectedPrompt == prompt) 
                                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                                    else null
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectedPrompt == prompt,
                                            onClick = { selectedPrompt = prompt }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(prompt.name, style = MaterialTheme.typography.titleMedium)
                                            Text(prompt.description, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            selectedPrompt?.let { prompt ->
                                isRunning = true
                                agentService.clearLogs()
                                scope.launch {
                                    val proposal = agentService.runAutoresearchPrompt(
                                        projectData = projectData,
                                        promptName = prompt.name,
                                        promptInstructions = prompt.instructions
                                    )
                                    finalProposal = proposal
                                    isRunning = false
                                }
                            }
                        },
                        enabled = selectedPrompt != null,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Execute Selected Research")
                    }
                }
            }
        }
    }
}
