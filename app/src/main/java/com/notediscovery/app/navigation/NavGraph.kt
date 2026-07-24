package com.notediscovery.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.notediscovery.app.data.model.NoteResponse
import com.notediscovery.app.data.repository.NotesRepository
import com.notediscovery.app.ui.screens.*
import com.notediscovery.app.viewmodel.*
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val LIST = "list"
    const val DETAIL = "detail/{encodedPath}"
    const val EDIT = "edit?path={path}&title={title}&content={content}&tags={tags}"
    const val SETTINGS = "settings"

    fun detail(path: String) = "detail/${URLEncoder.encode(path, "UTF-8")}"
    fun edit(path: String, title: String, content: String, tags: String) =
        "edit?path=${URLEncoder.encode(path, "UTF-8")}" +
        "&title=${URLEncoder.encode(title, "UTF-8")}" +
        "&content=${URLEncoder.encode(content, "UTF-8")}" +
        "&tags=${URLEncoder.encode(tags, "UTF-8")}"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    repository: NotesRepository,
    listViewModel: NoteListViewModel,
    detailViewModel: NoteDetailViewModel,
    editViewModel: NoteEditViewModel,
    settingsViewModel: SettingsViewModel
) {
    NavHost(navController = navController, startDestination = Routes.LIST) {

        composable(Routes.LIST) {
            val state = listViewModel.uiState.collectAsState().value
            NoteListScreen(
                notes = state.notes,
                isLoading = state.isLoading,
                error = state.error,
                searchQuery = state.searchQuery,
                onSearchQueryChange = { listViewModel.search(it) },
                onNoteClick = { note ->
                    navController.navigate(Routes.detail(note.path))
                },
                onCreateNote = {
                    editViewModel.initNew()
                    navController.navigate(
                        Routes.edit("", "", "", "")
                    )
                },
                onRefresh = { listViewModel.refresh() }
            )
        }

        composable(
            route = "detail/{encodedPath}",
            arguments = listOf(navArgument("encodedPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("encodedPath") ?: return@composable
            val path = URLDecoder.decode(encodedPath, "UTF-8")

            LaunchedEffect(path) { detailViewModel.loadNote(path) }
            val state = detailViewModel.uiState.collectAsState().value

            if (state.isDeleted) {
                LaunchedEffect(Unit) { navController.popBackStack() }
                return@composable
            }

            NoteDetailScreen(
                note = state.note,
                isLoading = state.isLoading,
                error = state.error,
                onBack = { navController.popBackStack() },
                onEdit = { note ->
                    editViewModel.initEdit(note.path, note.title, note.content, note.tags)
                    navController.navigate(
                        Routes.edit(note.path, note.title, note.content, note.tags.joinToString(","))
                    )
                },
                onDelete = {
                    detailViewModel.deleteNote(path)
                }
            )
        }

        composable(
            route = "edit?path={path}&title={title}&content={content}&tags={tags}",
            arguments = listOf(
                navArgument("path") { type = NavType.StringType; defaultValue = "" },
                navArgument("title") { type = NavType.StringType; defaultValue = "" },
                navArgument("content") { type = NavType.StringType; defaultValue = "" },
                navArgument("tags") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val path = backStackEntry.arguments?.getString("path") ?: ""
            val title = backStackEntry.arguments?.getString("title") ?: ""
            val content = backStackEntry.arguments?.getString("content") ?: ""
            val tags = backStackEntry.arguments?.getString("tags") ?: ""

            if (path.isNotBlank() && title.isNotBlank()) {
                editViewModel.initEdit(
                    URLDecoder.decode(path, "UTF-8"),
                    URLDecoder.decode(title, "UTF-8"),
                    URLDecoder.decode(content, "UTF-8"),
                    URLDecoder.decode(tags, "UTF-8").split(",").filter { it.isNotBlank() }
                )
            }

            val state = editViewModel.uiState.collectAsState().value

            if (state.saved) {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                    listViewModel.refresh()
                }
                return@composable
            }

            NoteEditScreen(
                title = state.title,
                content = state.content,
                tags = state.tags,
                isSaving = state.isSaving,
                saved = state.saved,
                error = state.error,
                onTitleChange = { editViewModel.updateTitle(it) },
                onContentChange = { editViewModel.updateContent(it) },
                onTagsChange = { editViewModel.updateTags(it) },
                onSave = { editViewModel.save() },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            val state = settingsViewModel.uiState.collectAsState().value
            SettingsScreen(
                serverUrl = state.serverUrl,
                apiKey = state.apiKey,
                testResult = state.testResult,
                isTesting = state.isTesting,
                onUrlChange = { settingsViewModel.updateUrl(it) },
                onKeyChange = { settingsViewModel.updateKey(it) },
                onSave = { settingsViewModel.save() },
                onTest = { settingsViewModel.testConnection() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
