package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MovieViewModel

enum class MainTab {
    INICIO,
    BUSCAR,
    SUBIR,
    MI_ESPACIO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHubScreen(
    viewModel: MovieViewModel,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToPlayer: (Int) -> Unit,
    onLogoutProfile: () -> Unit
) {
    var activeTab by remember { mutableStateOf(MainTab.INICIO) }
    val activeProfile by viewModel.currentProfile.collectAsState()

    BackHandler {
        if (activeTab != MainTab.INICIO) {
            activeTab = MainTab.INICIO
        } else {
            onLogoutProfile()
        }
    }

    Scaffold(
        containerColor = Color(0xFF09111E),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Styled Logo text resembling Prime branding
                        Text(
                            text = "prime",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "plex",
                            color = Color(0xFF00A8E1),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Active profile name chip preview
                        activeProfile?.let { profile ->
                            Box(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .background(
                                        color = AvatarColors.getOrElse(profile.avatarColorIndex) { AvatarColors[0] },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = profile.name,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F1E36)
                ),
                modifier = Modifier.testTag("main_hub_top_bar")
            )
        },
        bottomBar = {
            // Elegant navigation bar styled after the Prime Video design
            NavigationBar(
                containerColor = Color(0xFF0F1E36),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("app_bottom_bar")
            ) {
                // Inicio Item
                NavigationBarItem(
                    selected = activeTab == MainTab.INICIO,
                    onClick = { activeTab = MainTab.INICIO },
                    label = { Text("Inicio", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00A8E1),
                        selectedTextColor = Color(0xFF00A8E1),
                        indicatorColor = Color(0xFF09111E),
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.LightGray
                    ),
                    modifier = Modifier.testTag("nav_item_inicio")
                )

                // Buscar Item
                NavigationBarItem(
                    selected = activeTab == MainTab.BUSCAR,
                    onClick = { activeTab = MainTab.BUSCAR },
                    label = { Text("Buscar", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00A8E1),
                        selectedTextColor = Color(0xFF00A8E1),
                        indicatorColor = Color(0xFF09111E),
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.LightGray
                    ),
                    modifier = Modifier.testTag("nav_item_buscar")
                )

                // Subir Item
                NavigationBarItem(
                    selected = activeTab == MainTab.SUBIR,
                    onClick = { activeTab = MainTab.SUBIR },
                    label = { Text("Subir", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Subir enlace") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00A8E1),
                        selectedTextColor = Color(0xFF00A8E1),
                        indicatorColor = Color(0xFF09111E),
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.LightGray
                    ),
                    modifier = Modifier.testTag("nav_item_subir")
                )

                // Mi Espacio Item
                NavigationBarItem(
                    selected = activeTab == MainTab.MI_ESPACIO,
                    onClick = { activeTab = MainTab.MI_ESPACIO },
                    label = { Text("Mi Espacio", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Mi Espacio") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00A8E1),
                        selectedTextColor = Color(0xFF00A8E1),
                        indicatorColor = Color(0xFF09111E),
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.LightGray
                    ),
                    modifier = Modifier.testTag("nav_item_mi_espacio")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                MainTab.INICIO -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToDetail = onNavigateToDetail,
                        onNavigateToPlayer = onNavigateToPlayer
                    )
                }
                MainTab.BUSCAR -> {
                    SearchScreen(
                        viewModel = viewModel,
                        onNavigateToDetail = onNavigateToDetail
                    )
                }
                MainTab.SUBIR -> {
                    AddMovieScreen(
                        viewModel = viewModel
                    )
                }
                MainTab.MI_ESPACIO -> {
                    MyStuffScreen(
                        viewModel = viewModel,
                        onLogoutProfile = onLogoutProfile,
                        onNavigateToDetail = onNavigateToDetail
                    )
                }
            }
        }
    }
}
