package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.ImdbScoreCard
import com.example.ui.components.tvFocusable
import com.example.ui.viewmodel.MovieViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    movieId: Int,
    viewModel: MovieViewModel,
    onNavigateToPlayer: (Int, Int) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val movieState = viewModel.allMovies.collectAsState().value
    val movie = movieState.find { it.id == movieId }

    if (movie == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF09111E)),
            contentAlignment = Alignment.Center
        ) {
            Text("Película no encontrada", color = Color.White)
        }
        return
    }

    val isAdmin by viewModel.isAdmin.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val inWatchlist by viewModel.isMovieInWatchlist(movieId).collectAsState(initial = false)
    val savedProgress by viewModel.getMoviePlaybackProgress(movieId).collectAsState(initial = 0L)
    val progressDetails by viewModel.getMoviePlaybackProgressDetails(movieId).collectAsState(initial = null)
    val isRefreshingImdb by viewModel.isRefreshingImdb.collectAsState()
    val scrollState = rememberScrollState()
    val episodes = remember(movie.episodesJson) { movie.getEpisodes() }

    // Dialog to confirm deletion
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text("¿Eliminar de la biblioteca?", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "¿Estás seguro de que deseas eliminar permanentemente \"${movie.title}\" de la biblioteca?\n\nEsta acción no se puede deshacer.",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteMovie(movie)
                        android.widget.Toast.makeText(context, "\"${movie.title}\" ha sido eliminada", android.widget.Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                ) {
                    Text("Eliminar definitivamente", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF0F1E36)
        )
    }

    // Dialog to edit movie or series
    if (showEditDialog) {
        EditMovieDialog(
            movie = movie,
            viewModel = viewModel,
            onDismiss = { showEditDialog = false },
            onUpdated = { }
        )
    }

    Scaffold(
        containerColor = Color(0xFF09111E),
        topBar = {
            TopAppBar(
                title = { Text(movie.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .tvFocusable(shape = RoundedCornerShape(24.dp), focusedScale = 1.1f)
                            .testTag("detail_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F1E36)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            // Widescreen Movie Poster with Bottom Fade
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = movie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Dark Bottom fading overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF09111E).copy(alpha = 0.5f),
                                    Color(0xFF09111E)
                                )
                            )
                        )
                )
            }

            // Movie Details & Meta Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Movie metadata row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        text = movie.year,
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = movie.duration,
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = movie.genre,
                        color = Color(0xFF00A8E1),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(
                                Color(0xFF00A8E1).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = movie.category,
                        color = Color(0xFFFF9900),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(
                                Color(0xFFFF9900).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )

                    // IMDb Rating Gold Badge
                    if (movie.imdbRating.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFF5C518)
                        ) {
                            Text(
                                text = "IMDb ${movie.imdbRating} ★",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // VISTO Badge
                    if (progressDetails?.isWatched() == true) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF2BAD3B)
                        ) {
                            Text(
                                text = "✓ VISTO",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = movie.title,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val hasVideo = movie.videoUrl.isNotBlank()

                // Main Action Buttons
                val hasProgress = progressDetails != null && progressDetails!!.progressMs > 2000L

                if (hasProgress && hasVideo) {
                    // 1. Primary "Seguir viendo" Button with exact minute and second
                    Button(
                        onClick = { onNavigateToPlayer(movie.id, progressDetails?.episodeIndex ?: -1) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A94FF)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .tvFocusable(shape = RoundedCornerShape(8.dp), focusedScale = 1.04f)
                            .testTag("resume_movie_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Seguir viendo", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Seguir viendo desde ${progressDetails!!.formatProgressTime()}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Progress track with exact minute and total duration
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pausado en ${progressDetails!!.formatProgressTime()}" +
                                        if (progressDetails!!.formatDurationTime().isNotBlank()) " de ${progressDetails!!.formatDurationTime()}" else "",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "${(progressDetails!!.getProgressFraction() * 100).toInt()}% visto",
                                color = Color(0xFFFF9900),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(Color.Gray.copy(alpha = 0.35f), RoundedCornerShape(2.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progressDetails!!.getProgressFraction().coerceIn(0.02f, 1f))
                                    .background(Color(0xFFFF9900), RoundedCornerShape(2.dp))
                            )
                        }
                    }

                    // 2. Secondary "Empezar desde el principio" Button
                    OutlinedButton(
                        onClick = {
                            viewModel.updatePlaybackProgress(movie.id, 0L, progressDetails!!.durationMs)
                            onNavigateToPlayer(movie.id, 0)
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .tvFocusable(shape = RoundedCornerShape(8.dp), focusedScale = 1.04f)
                            .testTag("restart_movie_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF00A8E1), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Empezar desde el principio (0:00)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    // Standard Play Button when starting fresh
                    Button(
                        onClick = {
                            if (hasVideo) {
                                onNavigateToPlayer(movie.id, -1)
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    "No se puede reproducir: este título está guardado en tu biblioteca sin enlace de vídeo.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasVideo) Color(0xFF1A94FF) else Color(0xFF27354A),
                            contentColor = if (hasVideo) Color.White else Color.LightGray
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .tvFocusable(
                                shape = RoundedCornerShape(8.dp),
                                focusedBorderColor = Color.White,
                                focusedScale = 1.04f
                            )
                            .testTag("play_movie_button")
                    ) {
                        Icon(
                            if (!hasVideo) Icons.Default.Info else Icons.Default.PlayArrow,
                            contentDescription = "Reproducir"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (!hasVideo) "Sin enlace de reproducción disponible" else "Reproducir ahora",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // -------------------------------------------------------------
                // ADMIN ACTIONS PANEL (EDITAR / ELIMINAR)
                // Visible ONLY to the administrator (juanjocarrillo7@gmail.com)
                // -------------------------------------------------------------
                if (isAdmin) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF132238)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF00A8E1).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().testTag("admin_controls_panel")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF00A8E1),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Gestión de Administrador (juanjocarrillo7@gmail.com)",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { showEditDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A8E1)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f).testTag("admin_edit_button")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Editar Contenido", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { showDeleteConfirmDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f).testTag("admin_delete_button")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                if (!hasVideo) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2E4A).copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF00A8E1), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Ficha catalogada sin enlace de vídeo. Puedes consultar su sinopsis, reparto y episodios, pero no se puede reproducir.",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Saved progress bar indicator
                if (savedProgress > 0) {
                    Text(
                        text = "Tienes progreso guardado",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom control buttons rows
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Watchlist button
                    OutlinedButton(
                        onClick = { viewModel.toggleWatchlist(movie.id, !inWatchlist) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .tvFocusable(
                                shape = RoundedCornerShape(8.dp),
                                focusedBorderColor = Color(0xFF00A8E1),
                                focusedScale = 1.05f
                            )
                            .testTag("toggle_watchlist_button")
                    ) {
                        Icon(
                            if (inWatchlist) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null,
                            tint = if (inWatchlist) Color(0xFF00A8E1) else Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (inWatchlist) "En mi Lista" else "Mi Lista",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // For IMDb Links - open external IMDb title ficha
                    if (movie.imdbId.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                val imdbUrl = "https://www.imdb.com/title/${movie.imdbId}/"
                                val openIntent = Intent(Intent.ACTION_VIEW, Uri.parse(imdbUrl))
                                context.startActivity(openIntent)
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFF5C518)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFF5C518).copy(alpha = 0.6f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .tvFocusable(
                                    shape = RoundedCornerShape(8.dp),
                                    focusedBorderColor = Color(0xFFF5C518),
                                    focusedScale = 1.05f
                                )
                        ) {
                            Text(
                                "IMDb Ficha",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF5C518)
                            )
                        }
                    }

                    // For Google Drive links - open external fallback helper
                    if (movie.videoUrl.contains("drive.google.com")) {
                        OutlinedButton(
                            onClick = {
                                val openIntent = Intent(Intent.ACTION_VIEW, Uri.parse(movie.videoUrl))
                                context.startActivity(openIntent)
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("external_media_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.LightGray)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Drive",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Official Trailer Button
                if (movie.trailerUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            try {
                                val openIntent = Intent(Intent.ACTION_VIEW, Uri.parse(movie.trailerUrl))
                                context.startActivity(openIntent)
                            } catch (_: Exception) {
                                android.widget.Toast.makeText(context, "No se pudo abrir el tráiler", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .tvFocusable(shape = RoundedCornerShape(8.dp), focusedScale = 1.03f)
                            .testTag("watch_trailer_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Tráiler", tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🎬 Ver Tráiler Oficial", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                // Watched status toggle
                val isWatched = progressDetails?.isWatched() == true
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isWatched) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF2BAD3B).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFF2BAD3B))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2BAD3B), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Completada / Vista", color = Color(0xFF2BAD3B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        TextButton(onClick = { viewModel.clearPlaybackProgress(movie.id) }) {
                            Text("Desmarcar", color = Color.LightGray, fontSize = 12.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.markAsWatched(movie.id) },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                            border = BorderStroke(0.8.dp, Color.Gray.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2BAD3B), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Marcar como vista", fontSize = 12.sp)
                        }
                    }
                }

                // If playback progress exists, offer options to clear it
                if (savedProgress > 0 && !isWatched) {
                    TextButton(
                        onClick = { viewModel.clearPlaybackProgress(movie.id) },
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reiniciar progreso de visto", color = Color.Red, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Ficha Oficial de Calificación y Crítica de IMDb
                ImdbScoreCard(
                    movie = movie,
                    onRefreshRating = { viewModel.fetchImdbForMovie(movie.id) },
                    isRefreshing = isRefreshingImdb,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description Title (TMDB)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Sinopsis",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(Fuente: TMDB)",
                        color = Color(0xFF00A8E1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Description Text
                Text(
                    text = movie.description,
                    color = Color.LightGray,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Cast / Reparto (TMDB)
                if (movie.cast.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Reparto / Actores",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(TMDB)",
                            color = Color(0xFF00A8E1),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = movie.cast,
                        color = Color(0xFF00A8E1),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // -----------------------------------------------------------------
                // THETVDB ORDERED EPISODES SECTION (For Series)
                // -----------------------------------------------------------------
                if (movie.category == "Series" || episodes.isNotEmpty()) {
                    val distinctSeasons = remember(episodes) { episodes.map { it.seasonNumber }.distinct().sorted() }
                    var activeSeasonFilter by remember { mutableIntStateOf(distinctSeasons.firstOrNull() ?: 1) }
                    val displayedEpisodes = remember(episodes, activeSeasonFilter) {
                        if (distinctSeasons.size > 1) episodes.filter { it.seasonNumber == activeSeasonFilter } else episodes
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.List,
                                        contentDescription = null,
                                        tint = Color(0xFF2BAD3B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Guía de Episodios y Capítulos",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF2BAD3B).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        "${episodes.size} capítulos",
                                        color = Color(0xFF2BAD3B),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Season selector pills if more than 1 season exists
                            if (distinctSeasons.size > 1) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    distinctSeasons.forEach { sNum ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (activeSeasonFilter == sNum) Color(0xFF00A8E1) else Color(0xFF1E2E4A),
                                            modifier = Modifier
                                                .clickable { activeSeasonFilter = sNum }
                                        ) {
                                            Text(
                                                text = "Temporada $sNum",
                                                color = if (activeSeasonFilter == sNum) Color.Black else Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (displayedEpisodes.isEmpty()) {
                                Text(
                                    "Esta serie está registrada en el catálogo. Utiliza el reproductor para ver la transmisión principal.",
                                    color = Color.LightGray,
                                    fontSize = 13.sp
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    displayedEpisodes.forEach { ep ->
                                        val epIdx = episodes.indexOf(ep)
                                        val epHasVideo = ep.videoUrl.isNotBlank() || hasVideo

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF1E2E4A),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .tvFocusable(shape = RoundedCornerShape(8.dp), focusedScale = 1.02f)
                                                .clickable {
                                                    if (epHasVideo) {
                                                        onNavigateToPlayer(movie.id, epIdx)
                                                    } else {
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            "Este capítulo aún no tiene enlace asignado. El administrador puede editarlo.",
                                                            android.widget.Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .background(
                                                            if (epHasVideo) Color(0xFF00A8E1).copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f),
                                                            RoundedCornerShape(18.dp)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        if (epHasVideo) Icons.Default.PlayArrow else Icons.Default.Info,
                                                        contentDescription = "Reproducir capítulo",
                                                        tint = if (epHasVideo) Color(0xFF00A8E1) else Color.LightGray,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(12.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "T${ep.seasonNumber}E${ep.episodeNumber}: ${ep.title}",
                                                        color = Color.White,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )

                                                    val isThisEpisode = progressDetails != null &&
                                                            ep.seasonNumber == progressDetails!!.seasonNumber &&
                                                            ep.episodeNumber == progressDetails!!.episodeNumber &&
                                                            (progressDetails!!.progressMs > 2000L || progressDetails!!.isWatched())

                                                    if (isThisEpisode) {
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        val fraction = progressDetails!!.getProgressFraction()
                                                        val isEpWatched = progressDetails!!.isWatched()
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            if (isEpWatched) {
                                                                Surface(
                                                                    shape = RoundedCornerShape(4.dp),
                                                                    color = Color(0xFF2BAD3B)
                                                                ) {
                                                                    Row(
                                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                        verticalAlignment = Alignment.CenterVertically
                                                                    ) {
                                                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                                                                        Spacer(modifier = Modifier.width(3.dp))
                                                                        Text(
                                                                            text = "VISTO",
                                                                            color = Color.White,
                                                                            fontSize = 9.sp,
                                                                            fontWeight = FontWeight.Bold
                                                                        )
                                                                    }
                                                                }
                                                            } else {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .width(70.dp)
                                                                        .height(4.dp)
                                                                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                                                                ) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .fillMaxHeight()
                                                                            .fillMaxWidth(fraction.coerceIn(0.05f, 1f))
                                                                            .background(Color(0xFFFF9900), RoundedCornerShape(2.dp))
                                                                    )
                                                                }
                                                                Text(
                                                                    text = "Progreso: ${progressDetails!!.formatProgressTime()} (${(fraction * 100).toInt()}% visto)",
                                                                    color = Color(0xFFFF9900),
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                    }

                                                    if (ep.overview.isNotBlank()) {
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = ep.overview,
                                                            color = Color.Gray,
                                                            fontSize = 11.sp,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(8.dp))

                                                Text(
                                                    text = ep.runtime,
                                                    color = Color.LightGray,
                                                    fontSize = 11.sp,
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

                Spacer(modifier = Modifier.height(8.dp))

                // Metadata provider specs card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF00A8E1),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Bibliotecas Sincronizadas",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• The Movie Database (TMDB): Sinopsis en español y ficha de actores.\n" +
                                    "• IMDb: Valoraciones y código oficial (${if (movie.imdbId.isNotBlank()) movie.imdbId else "Registrado"}).\n" +
                                    "• TheTVDB: Orden oficial de temporadas y episodios.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (movie.videoUrl.contains("drive.google.com")) "Transmisión: Enlace Google Drive" else "Transmisión: Servidor Multimedia Directo",
                            color = Color(0xFF00A8E1),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
