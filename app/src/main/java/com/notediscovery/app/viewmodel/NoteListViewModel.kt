package com.notediscovery.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notediscovery.app.data.model.NoteSummary
import com.notediscovery.app.data.repository.NotesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NoteListUiState(
    val notes: List<NoteSummary> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = ""
)

class NoteListViewModel(
    private val repository: NotesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteListUiState())
    val uiState: StateFlow<NoteListUiState> = _uiState.asStateFlow()

    init { loadNotes() }

    fun loadNotes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getNotes().fold(
                onSuccess = { notes ->
                    _uiState.value = _uiState.value.copy(
                        notes = notes, isLoading = false, error = null
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false, error = e.message ?: "Ошибка загрузки"
                    )
                }
            )
        }
    }

    fun refresh() { loadNotes() }

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        if (query.isBlank()) {
            loadNotes()
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.search(query).fold(
                onSuccess = { results ->
                    val notes = results.map { r ->
                        NoteSummary(
                            path = r.url.removePrefix(repository.testConnection().getOrNull().orEmpty()),
                            title = r.title,
                            updatedAt = ""
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        notes = if (notes.isEmpty()) _uiState.value.notes else notes,
                        isLoading = false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false, error = e.message
                    )
                }
            )
        }
    }
}
