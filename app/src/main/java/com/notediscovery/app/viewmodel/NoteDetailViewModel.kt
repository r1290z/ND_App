package com.notediscovery.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notediscovery.app.data.model.NoteResponse
import com.notediscovery.app.data.repository.NotesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NoteDetailUiState(
    val note: NoteResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isDeleted: Boolean = false
)

class NoteDetailViewModel(
    private val repository: NotesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteDetailUiState())
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()

    fun loadNote(path: String) {
        viewModelScope.launch {
            _uiState.value = NoteDetailUiState(isLoading = true)
            repository.getNote(path).fold(
                onSuccess = { note ->
                    _uiState.value = NoteDetailUiState(note = note)
                },
                onFailure = { e ->
                    _uiState.value = NoteDetailUiState(
                        error = e.message ?: "Ошибка загрузки заметки"
                    )
                }
            )
        }
    }

    fun deleteNote(path: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.deleteNote(path).fold(
                onSuccess = {
                    _uiState.value = NoteDetailUiState(isDeleted = true)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Ошибка удаления"
                    )
                }
            )
        }
    }
}
