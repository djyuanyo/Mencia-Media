package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.AppDatabase
import com.example.data.repository.MovieRepository
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.MainHubScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.ProfileSelectionScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MovieViewModel
import com.example.ui.viewmodel.MovieViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize SQLite Room Database & Repository Pattern
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = MovieRepository(database.movieDao())
        
        // Setup ViewModel Factory and initialize central State Engine
        val viewModelFactory = MovieViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[MovieViewModel::class.java]

        setContent {
            MyApplicationTheme {
                val isInitialized by viewModel.isInitialized.collectAsState()
                val currentProfile by viewModel.currentProfile.collectAsState()

                if (!isInitialized) {
                    // Loading splash screen while Room database populated
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF09111E)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF00A8E1))
                    }
                } else {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = if (currentProfile == null) "profile_selection" else "main_hub",
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 1. Profile Selection Screen
                        composable("profile_selection") {
                            ProfileSelectionScreen(
                                viewModel = viewModel,
                                onProfileSelected = {
                                    navController.navigate("main_hub") {
                                        popUpTo("profile_selection") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 2. Main Platform Hub (Initiated Home, Search, Upload, Space)
                        composable("main_hub") {
                            MainHubScreen(
                                viewModel = viewModel,
                                onNavigateToDetail = { movieId ->
                                    navController.navigate("detail/$movieId")
                                },
                                onNavigateToPlayer = { movieId ->
                                    navController.navigate("player/$movieId")
                                },
                                onLogoutProfile = {
                                    viewModel.selectProfile(null)
                                    navController.navigate("profile_selection") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 3. Movie Details Screen Display
                        composable(
                            route = "detail/{movieId}",
                            arguments = listOf(navArgument("movieId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val movieId = backStackEntry.arguments?.getInt("movieId") ?: 0
                            DetailScreen(
                                movieId = movieId,
                                viewModel = viewModel,
                                onNavigateToPlayer = { id ->
                                    navController.navigate("player/$id")
                                },
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // 4. Immersive Landscape Media Video Player Controller
                        composable(
                            route = "player/{movieId}",
                            arguments = listOf(navArgument("movieId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val movieId = backStackEntry.arguments?.getInt("movieId") ?: 0
                            PlayerScreen(
                                movieId = movieId,
                                viewModel = viewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
