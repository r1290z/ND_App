package com.notediscovery.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notediscovery.app.data.model.NoteSummary

data class FolderItem(
    val name: String,
    val notes: List<NoteSummary>,
    val subfolders: Map<String, FolderItem> = emptyMap()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    notes: List<NoteSummary>,
    isLoading: Boolean,
    error: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNoteClick: (NoteSummary) -> Unit,
    onCreateNote: () -> Unit,
    onRefresh: () -> Unit
) {
    // Group notes by folder
    val folderStructure = remember(notes) { buildFolderStructure(notes) }
    val rootNotes = remember(notes) { notes.filter { "/" !in it.path } }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                placeholder = { Text("Поиск заметок...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Поиск") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true
            )

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(error, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = onRefresh) { Text("Повторить") }
                        }
                    }
                }
                notes.isEmpty() && folderStructure.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Нет заметок", style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Spacer(Modifier.height(4.dp))
                            Text("Нажми + чтобы создать", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        // Root-level notes (no folder)
                        if (rootNotes.isNotEmpty()) {
                            item {
                                Text("Корень",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                            items(rootNotes, key = { it.path }) { note ->
                                NoteCard(note = note, onClick = { onNoteClick(note) })
                            }
                        }

                        // Folders
                        folderStructure.forEach { (folderName, folder) ->
                            item(key = "folder_$folderName") {
                                FolderSection(
                                    name = folderName,
                                    folder = folder,
                                    onNoteClick = onNoteClick
                                )
                            }
                        }
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = onCreateNote,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Создать", tint = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
fun FolderSection(
    name: String,
    folder: FolderItem,
    onNoteClick: (NoteSummary) -> Unit,
    depth: Int = 0
) {
    var expanded by remember { mutableStateOf(depth == 0) }

    Column(modifier = Modifier.padding(start = (depth * 16).dp)) {
        // Folder header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                contentDescription = if (expanded) "Свернуть" else "Развернуть",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = "Папка",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${folder.notes.size + folder.subfolders.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }

        // Notes in this folder
        AnimatedVisibility(visible = expanded) {
            Column {
                folder.notes.forEach { note ->
                    NoteCard(note = note, onClick = { onNoteClick(note) },
                        modifier = Modifier.padding(start = (depth * 16 + 8).dp))
                }
                // Subfolders
                folder.subfolders.forEach { (subName, subFolder) ->
                    FolderSection(
                        name = subName,
                        folder = subFolder,
                        onNoteClick = onNoteClick,
                        depth = depth + 1
                    )
                }
            }
        }
    }
}

@Composable
fun NoteCard(note: NoteSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // Extract display name (filename without folder path)
    val displayName = note.title.ifBlank {
        note.path.split("/").last().removeSuffix(".md").replace("_", " ")
    }
    val folderPath = note.path.split("/").dropLast(1).joinToString("/")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (note.updatedAt.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = note.updatedAt.take(10),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

/**
 * Build a nested folder structure from flat note list.
 */
fun buildFolderStructure(notes: List<NoteSummary>): Map<String, FolderItem> {
    val folders = mutableMapOf<String, MutableList<NoteSummary>>()
    val subfolderMap = mutableMapOf<String, MutableMap<String, MutableList<NoteSummary>>>()

    for (note in notes) {
        val parts = note.path.split("/")
        if (parts.size >= 2) {
            val rootFolder = parts[0]
            if (parts.size == 2) {
                // Direct child of root folder: Ai/note.md
                folders.getOrPut(rootFolder) { mutableListOf() }.add(note)
            } else {
                // Nested: Общие/Проекты/note.md
                val subPath = parts.drop(1).dropLast(1).joinToString("/")
                val subKey = "$rootFolder/$subPath"
                subfolderMap.getOrPut(rootFolder) { mutableMapOf() }
                    .getOrPut(subKey) { mutableListOf() }.add(note)
            }
        }
    }

    return folders.mapValues { (rootName, rootNotes) ->
        val subs = subfolderMap[rootName] ?: emptyMap()
        val subFolders = if (subs.isNotEmpty()) {
            subs.mapKeys { it.key.removePrefix("$rootName/") }
                .mapValues { (_, notes) ->
                    FolderItem(name = notes.first().path.split("/").dropLast(1).last(), notes = notes)
                }
        } else emptyMap()
        FolderItem(name = rootName, notes = rootNotes, subfolders = subFolders)
    }
}
