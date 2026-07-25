package com.notediscovery.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notediscovery.app.data.model.NoteSummary

data class FolderEntry(
    val name: String,
    val noteCount: Int,
    val subfolders: List<String> = emptyList()
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
    val folders = remember(notes) { groupByFolder(notes) }
    val currentFolder = remember { mutableStateOf<String?>(null) }
    val breadcrumb = remember { mutableStateOf(listOf("NoteDiscovery")) }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentFolder.value != null) {
                    IconButton(onClick = {
                        currentFolder.value = null
                        breadcrumb.value = listOf("NoteDiscovery")
                    }) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                }
                Text(
                    text = breadcrumb.value.last(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(8.dp))

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Поиск...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                leadingIcon = { Icon(Icons.Default.Search, "Поиск") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            Spacer(Modifier.height(12.dp))

            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(error, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onRefresh) { Text("Повторить") }
                    }
                }
                currentFolder.value == null -> {
                    // Homepage: show folders as grid cards
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(folders, key = { it.name }) { folder ->
                            FolderCard(
                                folder = folder,
                                onClick = {
                                    currentFolder.value = folder.name
                                    breadcrumb.value = breadcrumb.value + folder.name
                                }
                            )
                        }
                    }
                }
                else -> {
                    // Inside a folder: show notes list
                    val folderNotes = remember(notes, currentFolder.value) {
                        notes.filter { it.path.startsWith(currentFolder.value + "/") }
                    }
                    FolderNotesList(folderNotes, onNoteClick)
                }
            }
        }

        // FAB
        if (currentFolder.value != null) {
            FloatingActionButton(
                onClick = onCreateNote,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Создать", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
fun FolderCard(folder: FolderEntry, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "${folder.noteCount} заметк${if (folder.noteCount % 10 == 1 && folder.noteCount % 100 != 11) "а" else "и"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun FolderNotesList(notes: List<NoteSummary>, onNoteClick: (NoteSummary) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
        items(notes, key = { it.path }) { note ->
            NoteCardItem(note, onClick = { onNoteClick(note) })
        }
        if (notes.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Папка пуста",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
fun NoteCardItem(note: NoteSummary, onClick: () -> Unit) {
    val displayName = note.title.ifBlank {
        note.path.split("/").last().removeSuffix(".md").replace("_", " ")
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (note.updatedAt.isNotBlank()) {
                    Text(
                        text = note.updatedAt.take(10),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

/**
 * Group notes by their top-level folder
 */
fun groupByFolder(notes: List<NoteSummary>): List<FolderEntry> {
    val map = mutableMapOf<String, MutableList<NoteSummary>>()
    for (note in notes) {
        val parts = note.path.split("/")
        val folder = if (parts.size >= 2) parts[0] else "_root"
        map.getOrPut(folder) { mutableListOf() }.add(note)
    }
    return map.entries
        .filter { it.key != "_root" }
        .sortedBy { it.key.lowercase() }
        .map { (name, notes) ->
            FolderEntry(name = name, noteCount = notes.size)
        }
}
