package com.family.tree.ui.components.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.family.tree.core.ProjectData
import com.family.tree.core.ai.agent.AgentProposal
import com.family.tree.core.ai.agent.AgentService
import com.family.tree.core.ai.agent.PromptTemplate
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

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
    var researchJob by remember { mutableStateOf<Job?>(null) }
    var hasStarted by remember { mutableStateOf(false) }
    var logSearchQuery by remember { mutableStateOf("") }

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
                            val uriHandler = LocalUriHandler.current
                            val urlRegex = remember { "(https?://[\\w\\d\\.\\-\\/\\?\\=\\&\\%\\+]+)".toRegex() }
                            
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                item {
                                    val annotatedResults = buildAnnotatedString {
                                        append(finalProposal!!.results)
                                        urlRegex.findAll(finalProposal!!.results).forEach { match ->
                                            addStyle(
                                                style = SpanStyle(
                                                    color = Color(0xFF2196F3), 
                                                    textDecoration = TextDecoration.Underline
                                                ),
                                                start = match.range.first,
                                                end = match.range.last + 1
                                            )
                                            addStringAnnotation(
                                                tag = "URL",
                                                annotation = match.value,
                                                start = match.range.first,
                                                end = match.range.last + 1
                                            )
                                        }
                                    }
                                    
                                    ClickableText(
                                        text = annotatedResults,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        onClick = { offset ->
                                            annotatedResults.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                                .firstOrNull()?.let { annotation ->
                                                    uriHandler.openUri(annotation.item)
                                                }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { 
                            finalProposal = null 
                        }) {
                            Text("Show Logs")
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = { 
                            finalProposal = null
                            hasStarted = false
                            agentService.clearLogs() 
                        }) {
                            Text("Back to Prompts")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { onDismissRequest() }) {
                            Text("Finish")
                        }
                    }
                } else if (isRunning || hasStarted) {
                    // Phase 2: Researching / Progress
                    val title = if (isRunning) "Research in Progress" else "Research Finished"
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    Text("Current Task: ${selectedPrompt?.name}", style = MaterialTheme.typography.bodyMedium)
                    
                    if (isRunning) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("The AI agent is consulting methodology guides and searching the archives...", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text("Research completed.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Agent Output Console:", style = MaterialTheme.typography.labelMedium)
                        
                        val clipboardManager = LocalClipboardManager.current
                        Row {
                            TextButton(
                                onClick = {
                                    val allLogs = logs.joinToString("\n")
                                    clipboardManager.setText(AnnotatedString(allLogs))
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Logs", style = MaterialTheme.typography.labelSmall)
                            }
                            
                            if (isRunning) {
                                TextButton(
                                    onClick = { researchJob?.cancel() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Stop Research", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = logSearchQuery,
                        onValueChange = { logSearchQuery = it },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        placeholder = { Text("Search logs...", style = MaterialTheme.typography.bodySmall) },
                        leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                        trailingIcon = { 
                            if (logSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { logSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, null, Modifier.size(18.dp))
                                }
                            }
                        },
                        textStyle = MaterialTheme.typography.bodySmall,
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                    
                    val filteredLogs = remember(logs, logSearchQuery) {
                        if (logSearchQuery.isBlank()) logs
                        else logs.filter { it.contains(logSearchQuery, ignoreCase = true) }
                    }
                    
                    val listState = rememberLazyListState()
                    val uriHandler = LocalUriHandler.current
                    val urlRegex = remember { "(https?://[\\w\\d\\.\\-\\/\\?\\=\\&\\%\\+]+)".toRegex() }
                    
                    LaunchedEffect(logs.size) {
                        if (logs.isNotEmpty() && logSearchQuery.isBlank()) {
                            listState.animateScrollToItem(logs.size - 1)
                        }
                    }
                    
                    SelectionContainer {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium)
                                .padding(8.dp)
                        ) {
                            LazyColumn(state = listState) {
                                items(filteredLogs) { logMsg ->
                                    val annotatedLog = buildAnnotatedString {
                                        // 1. Base Text
                                        append(logMsg)
                                        
                                        // 2. Apply Search Highlighting
                                        if (logSearchQuery.isNotBlank()) {
                                            var start = 0
                                            while (start < logMsg.length) {
                                                val index = logMsg.indexOf(logSearchQuery, start, ignoreCase = true)
                                                if (index == -1) break
                                                addStyle(
                                                    style = SpanStyle(
                                                        background = MaterialTheme.colorScheme.primaryContainer,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    start = index,
                                                    end = index + logSearchQuery.length
                                                )
                                                start = index + logSearchQuery.length
                                            }
                                        }
                                        
                                        // 3. Apply URL Styling and Annotations
                                        urlRegex.findAll(logMsg).forEach { match ->
                                            addStyle(
                                                style = SpanStyle(
                                                    color = Color(0xFF2196F3), // Nice blue for links
                                                    textDecoration = TextDecoration.Underline
                                                ),
                                                start = match.range.first,
                                                end = match.range.last + 1
                                            )
                                            addStringAnnotation(
                                                tag = "URL",
                                                annotation = match.value,
                                                start = match.range.first,
                                                end = match.range.last + 1
                                            )
                                        }
                                    }
                                    
                                    ClickableText(
                                        text = annotatedLog,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        onClick = { offset ->
                                            annotatedLog.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                                .firstOrNull()?.let { annotation ->
                                                    uriHandler.openUri(annotation.item)
                                                }
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                    
                    if (!isRunning) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { 
                                finalProposal = null
                                hasStarted = false
                                agentService.clearLogs() 
                            }, 
                            modifier = Modifier.align(Alignment.End)
                        ) {
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
                                hasStarted = true
                                agentService.clearLogs()
                                researchJob = scope.launch {
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
