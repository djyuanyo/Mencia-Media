package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.tvFocusable
import com.example.ui.viewmodel.MovieViewModel

@Composable
fun MyStuffScreen(
    viewModel: MovieViewModel,
    onLogoutProfile: () -> Unit,
    onNavigateToDetail: (Int) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var showAddProfileDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableStateOf(0) }
    var isKidProfile by remember { mutableStateOf(false) }

    val activeProfile by viewModel.currentProfile.collectAsState()
    val userProfiles by viewModel.profiles.collectAsState()
    val currentUserAccount by viewModel.currentUserAccount.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val watchlist by viewModel.watchlist.collectAsState()
    val continueWatching by viewModel.continueWatching.collectAsState()
    val allMovies by viewModel.allMovies.collectAsState()

    val profileNonNull = activeProfile ?: return

    val color = remember(profileNonNull) {
        AvatarColors.getOrElse(profileNonNull.avatarColorIndex) { AvatarColors[0] }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09111E))
            .padding(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. ACTIVE PROFILE HEADER HERO CARD
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("profile_badge_card")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(color),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = profileNonNull.name.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profileNonNull.name,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            currentUserAccount?.let { acc ->
                                Text(
                                    text = acc.email,
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isAdmin) Color(0xFF2BAD3B).copy(alpha = 0.2f) else Color(0xFF00A8E1).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (isAdmin) "✓ Administrador Oficial" else "✓ Usuario Registrado",
                                    color = if (isAdmin) Color(0xFF2BAD3B) else Color(0xFF00A8E1),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onLogoutProfile,
                            modifier = Modifier
                                .tvFocusable(shape = RoundedCornerShape(20.dp), focusedScale = 1.15f)
                                .testTag("logout_profile_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Cerrar Sesión / Cambiar Cuenta",
                                tint = Color(0xFFFF6B6B)
                            )
                        }
                    }
                }
            }

            // 1.5 PERFILES DE MI CUENTA (Múltiples perfiles por usuario)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("user_profiles_section")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Perfiles de mi Cuenta",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${userProfiles.size} perfil${if (userProfiles.size != 1) "es" else ""} disponible${if (userProfiles.size != 1) "s" else ""}",
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                            }
                            TextButton(
                                onClick = {
                                    newProfileName = ""
                                    selectedColorIndex = userProfiles.size % AvatarColors.size
                                    isKidProfile = false
                                    showAddProfileDialog = true
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF00A8E1), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Crear Perfil", color = Color(0xFF00A8E1), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(userProfiles) { p ->
                                val pColor = AvatarColors.getOrElse(p.avatarColorIndex) { AvatarColors[0] }
                                val isSelected = p.id == profileNonNull.id
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFF00A8E1).copy(alpha = 0.2f) else Color(0xFF1E2E4A),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF00A8E1) else Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .clickable { viewModel.selectProfile(p) }
                                        .tvFocusable(shape = RoundedCornerShape(10.dp))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(pColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = p.name.take(1).uppercase(),
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = p.name,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (isSelected) {
                                                Text(
                                                    text = "Activo",
                                                    color = Color(0xFF00A8E1),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. CONTINUE WATCHING IN MY SPACE
            if (continueWatching.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "Por ver pronto",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(continueWatching) { movie ->
                                LandscapeMovieCard(movie, onNavigateToDetail)
                            }
                        }
                    }
                }
            }

            // 3. WATCHLIST
            item {
                Column {
                    Text(
                        text = "Mi lista de seguimiento (${watchlist.size})",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    if (watchlist.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2E4A).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Tu lista de seguimiento está vacía", color = Color.LightGray, fontSize = 14.sp)
                                    Text("Añade películas usando el botón '+' en los detalles.", color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(watchlist) { movie ->
                                LandscapeMovieCard(movie, onNavigateToDetail)
                            }
                        }
                    }
                }
            }

            // 4. STATS METRICS SUMMARY
            item {
                Column {
                    Text(
                        text = "Estadísticas del Servidor Plex",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Películas Totales", color = Color.Gray, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${allMovies.size} títulos", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Siguiendo", color = Color.Gray, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${watchlist.size} ítems", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Bottom Spacer
            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        if (showAddProfileDialog) {
            AlertDialog(
                onDismissRequest = { showAddProfileDialog = false },
                title = { Text("Crear Nuevo Perfil", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Introduce un nombre para el nuevo perfil:", color = Color.LightGray, fontSize = 13.sp)
                        OutlinedTextField(
                            value = newProfileName,
                            onValueChange = { newProfileName = it },
                            placeholder = { Text("Nombre (ej. Niños, Ana...)", color = Color.Gray) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E2E4A),
                                unfocusedContainerColor = Color(0xFF1E2E4A),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isKidProfile,
                                onCheckedChange = { isKidProfile = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00A8E1))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Perfil infantil (Contenido familiar)", color = Color.LightGray, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newProfileName.trim().isNotBlank()) {
                                viewModel.createProfile(newProfileName.trim(), selectedColorIndex, isKidProfile)
                                Toast.makeText(context, "Perfil \"${newProfileName.trim()}\" creado", Toast.LENGTH_SHORT).show()
                                showAddProfileDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A8E1))
                    ) {
                        Text("Crear", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddProfileDialog = false }) {
                        Text("Cancelar", color = Color.LightGray)
                    }
                },
                containerColor = Color(0xFF0F1E36)
            )
        }
    }
}
