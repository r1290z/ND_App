package com.notediscovery.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notediscovery.app.data.repository.NotesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NoteEditUiState(
    val title: String = "",
    val content: String = "",
    val tags: String = "",
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)

class NoteEditViewModel(
    private val repository: NotesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteEditUiState())
    val uiState: StateFlow<NoteEditUiState> = _uiState.asStateFlow()

    private var editPath: String? = null

    fun initNew() {
        _uiState.value = NoteEditUiState()
        editPath = null
    }

    fun initEdit(path: String, title: String, content: String, tags: List<String>) {
        editPath = path
        _uiState.value = NoteEditUiState(
            title = title,
            content = content,
            tags = tags.joinToString(", ")
        )
    }

    fun updateTitle(t: String) { _uiState.value = _uiState.value.copy(title = t) }
    fun updateContent(c: String) { _uiState.value = _uiState.value.copy(content = c) }
    fun updateTags(t: String) { _uiState.value = _uiState.value.copy(tags = t) }

    fun save() {
        val state = _uiState.value
        if (state.title.isBlank() && state.content.isBlank()) return

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            val content = "# ${state.title}\n\n${state.content}"
            val path = editPath ?: "Mobile/${state.title.replace(" ", "_")}.md"

            val result = if (editPath != null) {
                repository.updateNote(path, content)
            } else {
                repository.createNote(path, content)
            }

            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isSaving = false, saved = true)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = e.message ?: "Ошибка сохранения"
                    )
                }
            )
        }
    }
}
