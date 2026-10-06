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
    var showMacDialog by remember { mutableStateOf(false) }

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

            // 5. MACOS INSTALLATION CARD (.DMG)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F223D)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00A8E1).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().testTag("mac_installation_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🍏", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Instalador .DMG para macOS",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "PrimePlex-macOS.dmg (MacBook, iMac, Mac mini)",
                                    color = Color(0xFF00A8E1),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Se ha generado el instalador oficial 'PrimePlex-macOS.dmg' para instalar la aplicación directamente en tu Mac sin emuladores:\n" +
                                    "• Paquete instalador de disco Apple (.dmg) con PrimePlex.app.\n" +
                                    "• Compatible con macOS Sonoma, Ventura, Monterey (chips M1, M2, M3, M4 e Intel).",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showMacDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A8E1)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Instalación .DMG en Mac", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val url = "https://ais-pre-5fs77qi7v7kfvi54tjkqds-42077592416.europe-west2.run.app"
                                    clipboardManager.setText(AnnotatedString(url))
                                    Toast.makeText(context, "Enlace copiado al portapapeles", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copiar Enlace Web", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 6. INTUITIVE HELP TIP DESIGN
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2E4A).copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF00A8E1), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Soporte de Enlaces de Nube", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Puedes añadir contenido desde WordPress o Google Drive de forma libre. Si un vídeo de Google Drive se detiene o muestra un error en la reproducción, asegúrate de activar la opción 'Compartir enlace' con visualización pública, o selecciona 'Abrir Drive' para reproducirla de forma externa de manera garantizada.",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Bottom Spacer
            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        // DIALOG: DETAILED MACOS INSTALLATION GUIDE
        if (showMacDialog) {
            AlertDialog(
                onDismissRequest = { showMacDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🍏", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Instalador PrimePlex para macOS (.DMG)",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    val dialogScroll = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(dialogScroll)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF00A8E1).copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "💾 INSTALACIÓN CON 'PrimePlex-macOS.dmg'",
                                    color = Color(0xFF00A8E1),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "1. El archivo 'PrimePlex-macOS.dmg' está generado en la raíz de tu proyecto.\n\n" +
                                            "2. En tu Mac, haz doble clic sobre 'PrimePlex-macOS.dmg'. Se montará una ventana de instalación con el icono de PrimePlex y la carpeta Aplicaciones.\n\n" +
                                            "3. Arrastra 'PrimePlex.app' hacia 'Applications' (Aplicaciones).\n\n" +
                                            "4. ¡Listo! Ya tienes la app instalada en tu Mac. Puedes abrirla desde Launchpad o Spotlight directamente sin necesidad de emuladores.",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E2E4A),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "🚀 ALTERNATIVA DIRECTA: AÑADIR AL DOCK DE MAC",
                                    color = Color(0xFFFF9900),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Si prefieres tenerla instalada en 1 solo clic en tu Mac sin descargar archivos:\n\n" +
                                            "• En Safari (macOS): Abre el enlace de PrimePlex y haz clic en Archivo > 'Añadir al Dock'. Se creará una app nativa en tu Dock al instante.",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val url = "https://ais-pre-5fs77qi7v7kfvi54tjkqds-42077592416.europe-west2.run.app"
                                clipboardManager.setText(AnnotatedString(url))
                                Toast.makeText(context, "Enlace copiado al portapapeles", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2BAD3B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copiar Enlace Web para Safari en Mac", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showMacDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A8E1))
                    ) {
                        Text("Entendido", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color(0xFF0A182E)
            )
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
