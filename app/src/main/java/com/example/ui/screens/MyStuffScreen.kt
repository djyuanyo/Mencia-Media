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

    val activeProfile by viewModel.currentProfile.collectAsState()
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

            // 5. MACOS INSTALLATION CARD
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
                                    text = "Instalar PrimePlex en macOS",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "MacBook, iMac y Mac mini (Apple Silicon M1/M2/M3/M4 e Intel)",
                                    color = Color(0xFF00A8E1),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Tienes 2 opciones para instalar y usar PrimePlex en tu Mac con la mejor experiencia:\n" +
                                    "1. Como App nativa en tu Dock de macOS (Safari o Chrome).\n" +
                                    "2. Instalando el paquete APK en macOS mediante emulador nativo.",
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
                                Text("Ver Guía Paso a Paso", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                                Text("Copiar Enlace Mac", color = Color.White, fontSize = 12.sp)
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
                            text = "Cómo instalar PrimePlex en macOS",
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
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "⭐ MÉTODO 1 (Más Rápido): App en el Dock de Mac",
                                    color = Color(0xFF00A8E1),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Convierte la app en una aplicación nativa de macOS integrada con el Dock y la barra de menús:\n\n" +
                                            "• En Safari (macOS Sonoma 14+): Abre el enlace de la app y ve a Archivo > Añadir al Dock (Add to Dock). Tendrás el icono de PrimePlex en tu Dock y en la carpeta /Aplicaciones de tu Mac.\n\n" +
                                            "• En Google Chrome: Abre el enlace, haz clic en el icono de 'Instalar PrimePlex' en la barra de direcciones (o Menú > Guardar y compartir > Instalar PrimePlex como ventana).",
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
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "📦 MÉTODO 2: Instalar el paquete APK en Mac",
                                    color = Color(0xFFFF9900),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Si prefieres el ejecutable APK en tu Mac:\n\n" +
                                            "1. Descarga el APK desde el menú superior de Google AI Studio (botón de exportar / generar APK).\n" +
                                            "2. En macOS (Apple Silicon M1/M2/M3/M4 o Intel), puedes abrir e instalar el APK con:\n" +
                                            "   • Android Studio para Mac (Emulador oficial de Google).\n" +
                                            "   • MuMuPlayer Pro para Mac (altamente optimizado para macOS).\n" +
                                            "   • BlueStacks para Mac.\n" +
                                            "3. Arrastra el archivo APK a la ventana y se instalará automáticamente con aceleración gráfica completa.",
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
                            Text("Copiar Enlace Web para Safari/Chrome", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
    }
}
