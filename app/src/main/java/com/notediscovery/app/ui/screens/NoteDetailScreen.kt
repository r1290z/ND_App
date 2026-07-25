package com.notediscovery.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.notediscovery.app.data.model.NoteResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    note: NoteResponse?,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onEdit: (NoteResponse) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(note?.title ?: "Заметка", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (note != null) {
                        IconButton(onClick = { onEdit(note) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(error, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = onBack) { Text("Назад") }
                        }
                    }
                }
                note != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(8.dp))

                        // Folder path
                        val parts = note.path.split("/")
                        if (parts.size >= 2) {
                            Text(
                                text = "📍 ${parts.dropLast(1).joinToString(" › ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.height(4.dp))
                        }

                        if (note.updatedAt.isNotBlank()) {
                            Text(
                                text = "Обновлено: ${note.updatedAt.take(10)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.height(12.dp))
                        }

                        if (note.tags.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                note.tags.forEach { tag ->
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        )
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Spacer(Modifier.height(16.dp))

                        // Content with clickable links
                        val annotatedText = buildLinkText(note.content.ifBlank { "(пусто)" },
                            color = MaterialTheme.colorScheme.primary)

                        ClickableText(
                            text = annotatedText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                            ),
                            onClick = { offset ->
                                annotatedText.getStringAnnotations("link", offset, offset)
                                    .firstOrNull()?.let { annotation ->
                                        val link = annotation.item
                                        if (link.startsWith("http://") || link.startsWith("https://")) {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                                            context.startActivity(intent)
                                        } else if (link.startsWith("internal:")) {
                                            // Internal wiki link — could navigate to note
                                            // For now, just show it
                                        }
                                    }
                            }
                        )
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

/**
 * Parse content and make wiki-links and URLs clickable.
 * Supports:
 * - [[Note Name]] → internal wiki links
 * - https://... → external URLs
 * - www.... → external URLs
 */
fun buildLinkText(content: String, color: Color): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val regex = Regex("""\[\[([^\]]+)\]\]|(https?://[^\s)]+)|(www\.[^\s)]+)""")
    var lastIndex = 0

    for (match in regex.findAll(content)) {
        if (match.range.first > lastIndex) {
            builder.append(content.substring(lastIndex, match.range.first))
        }

        val wikiLink = match.groupValues[1]
        val httpUrl = match.groupValues[2]
        val wwwUrl = match.groupValues[3]

        when {
            wikiLink.isNotBlank() -> {
                builder.pushStringAnnotation("link", "internal:$wikiLink")
                builder.withStyle(SpanStyle(
                    color = color,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Bold
                )) { append(wikiLink) }
                builder.pop()
            }
            httpUrl.isNotBlank() -> {
                builder.pushStringAnnotation("link", httpUrl)
                builder.withStyle(SpanStyle(
                    color = color,
                    textDecoration = TextDecoration.Underline
                )) { append(httpUrl) }
                builder.pop()
            }
            wwwUrl.isNotBlank() -> {
                val fullUrl = "https://$wwwUrl"
                builder.pushStringAnnotation("link", fullUrl)
                builder.withStyle(SpanStyle(
                    color = color,
                    textDecoration = TextDecoration.Underline
                )) { append(wwwUrl) }
                builder.pop()
            }
        }
        lastIndex = match.range.last + 1
    }

    if (lastIndex < content.length) {
        builder.append(content.substring(lastIndex))
    }

    return builder.toAnnotatedString()
}
