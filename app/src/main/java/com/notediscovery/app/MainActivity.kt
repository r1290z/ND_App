package com.notediscovery.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.notediscovery.app.data.api.NoteDiscoveryClient
import com.notediscovery.app.data.repository.NotesRepository
import com.notediscovery.app.navigation.NavGraph
import com.notediscovery.app.ui.theme.NoteDiscoveryTheme
import com.notediscovery.app.viewmodel.*
import kotlinx.coroutines.launch
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val serverUrl = prefs.getString("server_url", "") ?: ""
        val apiKey = prefs.getString("api_key", "") ?: ""

        val client = NoteDiscoveryClient(serverUrl, apiKey)
        val repository = NotesRepository(client)

        // Handle share intent if app was just launched via share
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            handleShareIntent(intent, client, serverUrl, apiKey)
        }

        setContent {
            NoteDiscoveryTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // ViewModels
                val listVM: NoteListViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                            NoteListViewModel(repository) as T
                    }
                )
                val detailVM: NoteDetailViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                            NoteDetailViewModel(repository) as T
                    }
                )
                val editVM: NoteEditViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                            NoteEditViewModel(repository) as T
                    }
                )
                val settingsVM: SettingsViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                            SettingsViewModel(repository, this@MainActivity) as T
                    }
                )

                val showBottomBar = currentRoute in listOf("list", "settings")

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                NavigationBarItem(
                                    selected = currentRoute == "list",
                                    onClick = {
                                        if (currentRoute != "list") {
                                            navController.navigate("list") {
                                                popUpTo("list") { inclusive = true }
                                            }
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Заметки") },
                                    label = { Text("Заметки") }
                                )
                                NavigationBarItem(
                                    selected = currentRoute == "settings",
                                    onClick = {
                                        if (currentRoute != "settings") {
                                            navController.navigate("settings") {
                                                popUpTo("list")
                                            }
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Настройки") },
                                    label = { Text("Настройки") }
                                )
                            }
                        }
                    }
                ) { padding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        NavGraph(
                            navController = navController,
                            repository = repository,
                            listViewModel = listVM,
                            detailViewModel = detailVM,
                            editViewModel = editVM,
                            settingsViewModel = settingsVM
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            val serverUrl = prefs.getString("server_url", "") ?: ""
            val apiKey = prefs.getString("api_key", "") ?: ""
            val client = NoteDiscoveryClient(serverUrl, apiKey)
            handleShareIntent(intent, client, serverUrl, apiKey)
        }
    }

    private fun handleShareIntent(intent: Intent, client: NoteDiscoveryClient, serverUrl: String, apiKey: String) {
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return

        if (serverUrl.isEmpty() || apiKey.isEmpty()) {
            Toast.makeText(this, "⚙️ Сначала настрой сервер в приложении", Toast.LENGTH_LONG).show()
            return
        }

        // Extract URL from shared text
        val url = extractUrl(sharedText) ?: sharedText.trim()

        lifecycleScope.launch {
            try {
                val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
                val title = extractTitle(url)
                val safeTitle = title.replace(Regex("[^\\w\\s-]"), "").trim().take(60)
                    .replace(Regex("\\s+"), "_").ifEmpty { "clip_${System.currentTimeMillis()}" }

                val content = """
                    |# $title
                    |
                    |🔗 **Источник:** [$url]($url)
                    |
                    |📅 **Сохранено:** $dateStr
                    |
                    |---
                    |
                    |_Поделились из браузера. Открой на компьютере чтобы прочитать._
                """.trimMargin()

                val notePath = "Clipping/$safeTitle.md"
                val result = client.createNote(notePath, content)

                result.fold(
                    onSuccess = {
                        Toast.makeText(this@MainActivity, "📥 Сохранено: $title", Toast.LENGTH_LONG).show()
                    },
                    onFailure = { e ->
                        Toast.makeText(this@MainActivity, "❌ Ошибка: ${e.message?.take(80)}", Toast.LENGTH_LONG).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "❌ Ошибка: ${e.message?.take(80)}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun extractUrl(text: String): String? {
        // Try to find a URL in the shared text
        val urlPattern = Regex("https?://[^\\s]+")
        return urlPattern.find(text)?.value
    }

    private fun extractTitle(url: String): String {
        return try {
            val parsed = URL(url)
            val path = parsed.path.trim('/')
            val lastSegment = path.split("/").lastOrNull()
                ?.replace("-", " ")
                ?.replace("_", " ")
                ?.replace(Regex("\\.[a-z]+$"), "")
            val domain = parsed.host.replace("www.", "")
            // Try path first, fallback to domain
            if (!lastSegment.isNullOrBlank() && lastSegment.length > 5) {
                lastSegment.replaceFirstChar { it.uppercase() }
            } else {
                domain
            }
        } catch (e: Exception) {
            url.take(60)
        }
    }
}
