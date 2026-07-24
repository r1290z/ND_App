package com.notediscovery.app.data.api

import com.notediscovery.app.data.model.*
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class NoteDiscoveryClient(
    private var baseUrl: String,
    private var apiKey: String
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val mediaType = "application/json".toMediaType()

    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun updateConfig(url: String, key: String) {
        baseUrl = url.trimEnd('/')
        apiKey = key
    }

    fun getBaseUrl(): String = baseUrl
    fun getApiKey(): String = apiKey

    private fun requestBuilder(path: String): Request.Builder {
        return Request.Builder()
            .url("$baseUrl$path")
            .header("X-API-Key", apiKey)
    }

    suspend fun getNotes(): Result<NotesResponse> = runCatching {
        val request = requestBuilder("/api/notes").get().build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response")
        json.decodeFromString<NotesResponse>(body)
    }

    suspend fun getNote(path: String): Result<NoteResponse> = runCatching {
        val encodedPath = java.net.URLEncoder.encode(path, "UTF-8")
        val request = requestBuilder("/api/notes/$encodedPath").get().build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response")
        json.decodeFromString<NoteResponse>(body)
    }

    suspend fun createNote(path: String, content: String): Result<NoteResponse> = runCatching {
        val encodedPath = java.net.URLEncoder.encode(path, "UTF-8")
        val reqBody = json.encodeToString(SaveNoteRequest.serializer(), SaveNoteRequest(content))
            .toRequestBody(mediaType)
        val request = requestBuilder("/api/notes/$encodedPath").post(reqBody).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response")
        json.decodeFromString<NoteResponse>(body)
    }

    suspend fun updateNote(path: String, content: String): Result<NoteResponse> = runCatching {
        val encodedPath = java.net.URLEncoder.encode(path, "UTF-8")
        val reqBody = json.encodeToString(SaveNoteRequest.serializer(), SaveNoteRequest(content))
            .toRequestBody(mediaType)
        val request = requestBuilder("/api/notes/$encodedPath").put(reqBody).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response")
        json.decodeFromString<NoteResponse>(body)
    }

    suspend fun deleteNote(path: String): Result<Unit> = runCatching {
        val encodedPath = java.net.URLEncoder.encode(path, "UTF-8")
        val request = requestBuilder("/api/notes/$encodedPath").delete().build()
        client.newCall(request).execute()
        Unit
    }

    suspend fun appendNote(path: String, content: String, addTimestamp: Boolean = false): Result<NoteResponse> = runCatching {
        val encodedPath = java.net.URLEncoder.encode(path, "UTF-8")
        val reqBody = json.encodeToString(
            AppendNoteRequest.serializer(),
            AppendNoteRequest(content, addTimestamp)
        ).toRequestBody(mediaType)
        val request = requestBuilder("/api/notes/$encodedPath").patch(reqBody).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response")
        json.decodeFromString<NoteResponse>(body)
    }

    suspend fun search(query: String): Result<SearchResponse> = runCatching {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val request = requestBuilder("/api/search?q=$encodedQuery").get().build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response")
        json.decodeFromString<SearchResponse>(body)
    }

    suspend fun getTags(): Result<TagListResponse> = runCatching {
        val request = requestBuilder("/api/tags").get().build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response")
        json.decodeFromString<TagListResponse>(body)
    }

    suspend fun getStats(): Result<StatsResponse> = runCatching {
        val request = requestBuilder("/api/stats").get().build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response")
        json.decodeFromString<StatsResponse>(body)
    }

    suspend fun testConnection(): Result<String> = runCatching {
        val request = requestBuilder("/api/stats").get().build()
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val body = response.body?.string() ?: ""
            "OK (${response.code})"
        } else {
            throw Exception("HTTP ${response.code}: ${response.body?.string()}")
        }
    }
}
