package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.tvFocusable
import com.example.ui.viewmodel.MovieViewModel
import com.example.util.DeviceUtils

enum class MainTab {
    INICIO,
    BUSCAR,
    SUBIR,
    USUARIOS,
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
    val context = LocalContext.current
    val isTv = remember(context) { DeviceUtils.isTv(context) }
    var activeTab by remember { mutableStateOf(MainTab.INICIO) }
    val activeProfile by viewModel.currentProfile.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()

    LaunchedEffect(isAdmin) {
        if (!isAdmin && (activeTab == MainTab.SUBIR || activeTab == MainTab.USUARIOS)) {
            activeTab = MainTab.INICIO
        }
    }

    BackHandler {
        if (activeTab != MainTab.INICIO) {
            activeTab = MainTab.INICIO
        } else {
            onLogoutProfile()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09111E))
    ) {
        val isWideScreen = isTv || maxWidth >= 700.dp

        if (isWideScreen) {
            // ==========================================
            // GOOGLE TV / WIDESCREEN LIVING ROOM LAYOUT
            // Prime Video Navigation Rail on the left
            // ==========================================
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = Color(0xFF0F1E36),
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(108.dp)
                        .testTag("tv_navigation_rail"),
                    header = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(top = 20.dp, bottom = 12.dp)
                                .clickable { onLogoutProfile() }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "prime",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "plex",
                                    color = Color(0xFF00A8E1),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = Color(0xFF00A8E1),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "TV",
                                    color = Color.Black,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Inicio Tab
                    NavigationRailItem(
                        selected = activeTab == MainTab.INICIO,
                        onClick = { activeTab = MainTab.INICIO },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                        label = { Text("Inicio", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = Color(0xFF00A8E1),
                            selectedTextColor = Color(0xFF00A8E1),
                            indicatorColor = Color(0xFF09111E),
                            unselectedIconColor = Color.LightGray,
                            unselectedTextColor = Color.LightGray
                        ),
                        modifier = Modifier
                            .padding(vertical = 4.dp, horizontal = 8.dp)
                            .tvFocusable(shape = RoundedCornerShape(8.dp), focusedScale = 1.1f)
                            .testTag("nav_rail_inicio")
                    )

                    // Buscar Tab
                    NavigationRailItem(
                        selected = activeTab == MainTab.BUSCAR,
                        onClick = { activeTab = MainTab.BUSCAR },
                        icon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                        label = { Text("Buscar", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = Color(0xFF00A8E1),
                            selectedTextColor = Color(0xFF00A8E1),
                            indicatorColor = Color(0xFF09111E),
                            unselectedIconColor = Color.LightGray,
                            unselectedTextColor = Color.LightGray
                        ),
                        modifier = Modifier
                            .padding(vertical = 4.dp, horizontal = 8.dp)
                            .tvFocusable(shape = RoundedCornerShape(8.dp), focusedScale = 1.1f)
                            .testTag("nav_rail_buscar")
                    )

                    // Subir Tab (SOLO ADMINISTRADOR)
                    if (isAdmin) {
                        NavigationRailItem(
                            selected = activeTab == MainTab.SUBIR,
                            onClick = { activeTab = MainTab.SUBIR },
                            icon = { Icon(Icons.Default.Add, contentDescription = "Subir") },
                            label = { Text("Subir", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = Color(0xFF00A8E1),
                                selectedTextColor = Color(0xFF00A8E1),
                                indicatorColor = Color(0xFF09111E),
                                unselectedIconColor = Color.LightGray,
                                unselectedTextColor = Color.LightGray
                            ),
                            modifier = Modifier
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                                .tvFocusable(shape = RoundedCornerShape(8.dp), focusedScale = 1.1f)
                                .testTag("nav_rail_subir")
                        )

                        // Gestión de Usuarios Tab (SOLO ADMINISTRADOR)
                        NavigationRailItem(
                            selected = activeTab == MainTab.USUARIOS,
                            onClick = { activeTab = MainTab.USUARIOS },
                            icon = { Icon(Icons.Default.Person, contentDescription = "Usuarios") },
                            label = { Text("Usuarios", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = Color(0xFF00A8E1),
                                selectedTextColor = Color(0xFF00A8E1),
                                indicatorColor = Color(0xFF09111E),
                                unselectedIconColor = Color.LightGray,
                                unselectedTextColor = Color.LightGray
                            ),
                            modifier = Modifier
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                                .tvFocusable(shape = RoundedCornerShape(8.dp), focusedScale = 1.1f)
                                .testTag("nav_rail_usuarios")
                        )
                    }

                    // Mi Espacio Tab
                    NavigationRailItem(
                        selected = activeTab == MainTab.MI_ESPACIO,
                        onClick = { activeTab = MainTab.MI_ESPACIO },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Mi Espacio") },
                        label = { Text("Mi Espacio", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = Color(0xFF00A8E1),
                            selectedTextColor = Color(0xFF00A8E1),
                            indicatorColor = Color(0xFF09111E),
                            unselectedIconColor = Color.LightGray,
                            unselectedTextColor = Color.LightGray
                        ),
                        modifier = Modifier
                            .padding(vertical = 4.dp, horizontal = 8.dp)
                            .tvFocusable(shape = RoundedCornerShape(8.dp), focusedScale = 1.1f)
                            .testTag("nav_rail_mi_espacio")
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Profile Chip at bottom of rail
                    activeProfile?.let { profile ->
                        val avatarColor = AvatarColors.getOrElse(profile.avatarColorIndex) { AvatarColors[0] }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(bottom = 16.dp)
                                .tvFocusable(shape = RoundedCornerShape(12.dp), focusedScale = 1.1f)
                                .clickable { onLogoutProfile() }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(avatarColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = profile.name.take(1).uppercase(),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = profile.name,
                                color = Color.LightGray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // Main Content Panel on the right
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    when (activeTab) {
                        MainTab.INICIO -> HomeScreen(
                            viewModel = viewModel,
                            onNavigateToDetail = onNavigateToDetail,
                            onNavigateToPlayer = onNavigateToPlayer
                        )
                        MainTab.BUSCAR -> SearchScreen(
                            viewModel = viewModel,
                            onNavigateToDetail = onNavigateToDetail
                        )
                        MainTab.SUBIR -> AddMovieScreen(
                            viewModel = viewModel,
                            onMovieSaved = { activeTab = MainTab.INICIO }
                        )
                        MainTab.USUARIOS -> UserManagementScreen(
                            viewModel = viewModel
                        )
                        MainTab.MI_ESPACIO -> MyStuffScreen(
                            viewModel = viewModel,
                            onLogoutProfile = onLogoutProfile,
                            onNavigateToDetail = onNavigateToDetail
                        )
                    }
                }
            }
        } else {
            // ==========================================
            // COMPACT MOBILE PHONE LAYOUT
            // TopAppBar + Bottom NavigationBar
            // ==========================================
            Scaffold(
                containerColor = Color(0xFF09111E),
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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

                                activeProfile?.let { profile ->
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 12.dp)
                                            .background(
                                                color = AvatarColors.getOrElse(profile.avatarColorIndex) { AvatarColors[0] },
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { onLogoutProfile() }
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
                    NavigationBar(
                        containerColor = Color(0xFF0F1E36),
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("app_bottom_bar")
                    ) {
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
                            modifier = Modifier
                                .tvFocusable(shape = RoundedCornerShape(8.dp))
                                .testTag("nav_item_inicio")
                        )

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
                            modifier = Modifier
                                .tvFocusable(shape = RoundedCornerShape(8.dp))
                                .testTag("nav_item_buscar")
                        )

                        // Subir Tab (SOLO ADMINISTRADOR)
                        if (isAdmin) {
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
                                modifier = Modifier
                                    .tvFocusable(shape = RoundedCornerShape(8.dp))
                                    .testTag("nav_item_subir")
                            )

                            // Gestión de Usuarios Tab (SOLO ADMINISTRADOR)
                            NavigationBarItem(
                                selected = activeTab == MainTab.USUARIOS,
                                onClick = { activeTab = MainTab.USUARIOS },
                                label = { Text("Usuarios", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                icon = { Icon(Icons.Default.Person, contentDescription = "Gestión de Usuarios") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF00A8E1),
                                    selectedTextColor = Color(0xFF00A8E1),
                                    indicatorColor = Color(0xFF09111E),
                                    unselectedIconColor = Color.LightGray,
                                    unselectedTextColor = Color.LightGray
                                ),
                                modifier = Modifier
                                    .tvFocusable(shape = RoundedCornerShape(8.dp))
                                    .testTag("nav_item_usuarios")
                            )
                        }

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
                            modifier = Modifier
                                .tvFocusable(shape = RoundedCornerShape(8.dp))
                                .testTag("nav_item_mi_espacio")
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
                        MainTab.INICIO -> HomeScreen(
                            viewModel = viewModel,
                            onNavigateToDetail = onNavigateToDetail,
                            onNavigateToPlayer = onNavigateToPlayer
                        )
                        MainTab.BUSCAR -> SearchScreen(
                            viewModel = viewModel,
                            onNavigateToDetail = onNavigateToDetail
                        )
                        MainTab.SUBIR -> AddMovieScreen(
                            viewModel = viewModel,
                            onMovieSaved = { activeTab = MainTab.INICIO }
                        )
                        MainTab.USUARIOS -> UserManagementScreen(
                            viewModel = viewModel
                        )
                        MainTab.MI_ESPACIO -> MyStuffScreen(
                            viewModel = viewModel,
                            onLogoutProfile = onLogoutProfile,
                            onNavigateToDetail = onNavigateToDetail
                        )
                    }
                }
            }
        }
    }
}
