package com.notediscovery.app.data.repository

import com.notediscovery.app.data.api.NoteDiscoveryClient
import com.notediscovery.app.data.model.*

class NotesRepository(val client: NoteDiscoveryClient) {

    private var notesCache: List<NoteSummary> = emptyList()
    private var lastFetchTime = 0L
    private val cacheTtlMs = 30_000L

    suspend fun getNotes(forceRefresh: Boolean = false): Result<List<NoteSummary>> {
        val now = System.currentTimeMillis()
        if (!forceRefresh && notesCache.isNotEmpty() && (now - lastFetchTime) < cacheTtlMs) {
            return Result.success(notesCache)
        }
        return client.getNotes().map { response ->
            notesCache = response.notes
            lastFetchTime = now
            response.notes
        }
    }

    suspend fun getNote(path: String): Result<NoteResponse> {
        return client.getNote(path)
    }

    suspend fun createNote(path: String, content: String): Result<NoteResponse> {
        return client.createNote(path, content)
    }

    suspend fun updateNote(path: String, content: String): Result<NoteResponse> {
        return client.updateNote(path, content)
    }

    suspend fun deleteNote(path: String): Result<Unit> {
        notesCache = notesCache.filter { it.path != path }
        return client.deleteNote(path)
    }

    suspend fun search(query: String): Result<List<SearchResult>> {
        return client.search(query).map { it.results }
    }

    suspend fun getTags(): Result<List<TagInfo>> {
        return client.getTags().map { it.tags }
    }

    suspend fun getStats(): Result<StatsResponse> {
        return client.getStats()
    }

    suspend fun testConnection(): Result<String> {
        return client.testConnection()
    }

    fun updateConfig(url: String, apiKey: String) {
        client.updateConfig(url, apiKey)
        notesCache = emptyList()
        lastFetchTime = 0L
    }
}
