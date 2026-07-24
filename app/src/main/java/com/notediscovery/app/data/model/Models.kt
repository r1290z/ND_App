package com.notediscovery.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NoteResponse(
    val title: String = "",
    val content: String = "",
    val path: String = "",
    val tags: List<String> = emptyList(),
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class NotesResponse(
    val notes: List<NoteSummary> = emptyList(),
    val total: Int = 0
)

@Serializable
data class NoteSummary(
    val path: String = "",
    val title: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class SearchResult(
    val title: String = "",
    val url: String = "",
    val snippet: String = "",
    val score: Double = 0.0
)

@Serializable
data class SearchResponse(
    val results: List<SearchResult> = emptyList()
)

@Serializable
data class StatsResponse(
    @SerialName("note_count") val noteCount: Int = 0,
    @SerialName("tag_count") val tagCount: Int = 0,
    @SerialName("total_size") val totalSize: Long = 0
)

@Serializable
data class ErrorResponse(
    val detail: String = ""
)

@Serializable
data class SaveNoteRequest(
    val content: String
)

@Serializable
data class AppendNoteRequest(
    val content: String,
    @SerialName("add_timestamp") val addTimestamp: Boolean = false
)

@Serializable
data class TagListResponse(
    val tags: List<TagInfo> = emptyList()
)

@Serializable
data class TagInfo(
    val name: String = "",
    val count: Int = 0
)
