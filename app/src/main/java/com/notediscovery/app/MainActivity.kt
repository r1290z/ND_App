package com.notediscovery.app

import android.os.Bundle
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.notediscovery.app.data.api.NoteDiscoveryClient
import com.notediscovery.app.data.repository.NotesRepository
import com.notediscovery.app.navigation.NavGraph
import com.notediscovery.app.ui.theme.NoteDiscoveryTheme
import com.notediscovery.app.viewmodel.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val client = NoteDiscoveryClient(
            baseUrl = getSharedPreferences("settings", MODE_PRIVATE)
                .getString("server_url", "http://192.168.144.29:8000") ?: "http://192.168.144.29:8000",
            apiKey = getSharedPreferences("settings", MODE_PRIVATE)
                .getString("api_key", "f7271eca4d6525a35dcfd5a5d82641212bcad61891fa2948d2f828a259533000")
                ?: "f7271eca4d6525a35dcfd5a5d82641212bcad61891fa2948d2f828a259533000"
        )
        val repository = NotesRepository(client)

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
}
