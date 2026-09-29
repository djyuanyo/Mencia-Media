package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewModelScope
import com.example.data.model.Movie
import com.example.ui.viewmodel.MovieViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    movieId: Int,
    viewModel: MovieViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val movieState = viewModel.allMovies.collectAsState().value
    val movie = movieState.find { it.id == movieId }

    // Lock orientation to Landscape for cinematic viewing
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val previousOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = previousOrientation
        }
    }

    if (movie == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF09111E)),
            contentAlignment = Alignment.Center
        ) {
            Text("Pelicula no encontrada", color = Color.White)
        }
        return
    }

    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPos by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Auto-hide controls after 4 seconds
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    // Coroutine to periodically save playback progress in the database and feed slider
    LaunchedEffect(isPlaying, videoViewRef) {
        while (isPlaying && videoViewRef != null) {
            val progress = videoViewRef?.currentPosition?.toLong() ?: 0L
            val total = videoViewRef?.duration?.toLong() ?: 0L
            if (progress > 0) {
                currentPos = progress
                if (total > 0) {
                    duration = total
                    viewModel.updatePlaybackProgress(movieId, progress, total)
                }
            }
            delay(1000)
        }
    }

    // Process the video url
    val processedUrl = remember(movie.videoUrl) {
        cleanVideoUrl(movie.videoUrl)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("video_player_container")
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
            }
    ) {
        // Video View
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoURI(Uri.parse(processedUrl))
                    setOnPreparedListener { mp ->
                        isBuffering = false
                        duration = mp.duration.toLong()
                        
                        mp.start()
                        isPlaying = true
                        
                        // Fetch the starting position
                        viewModel.viewModelScope.launch {
                            val saved = viewModel.getMoviePlaybackProgress(movieId).first()
                            if (saved > 0 && saved < duration - 5000) {
                                seekTo(saved.toInt())
                                currentPos = saved
                            }
                        }
                    }
                    setOnErrorListener { _, _, extra ->
                        isBuffering = false
                        isPlaying = false
                        errorMessage = when (extra) {
                            -1004 -> "Error de conexión de red"
                            -1010 -> "Formato de video no soportado"
                            else -> "Este enlace requiere reproducción externa o navegador"
                        }
                        true
                    }
                    setOnCompletionListener {
                        isPlaying = false
                        viewModel.clearPlaybackProgress(movieId)
                        onNavigateBack()
                    }
                }
            },
            update = { view ->
                videoViewRef = view
            },
            modifier = Modifier.fillMaxSize()
        )

        // Buffering Indicator
        if (isBuffering) {
            CircularProgressIndicator(
                color = Color(0xFF1A94FF),
                modifier = Modifier
                    .align(Alignment.Center)
                    .testTag("buffering_indicator")
            )
        }

        // Error message handling
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = Color.Red,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No se pudo reproducir este medio directamente",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "URL: ${movie.videoUrl}",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Razón: ${errorMessage}. Enlaces de Google Drive necesitan ser públicos o directos. Puedes abrirlos externamente desde los detalles.",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        modifier = Modifier.widthIn(max = 500.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row {
                        Button(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                        ) {
                            Text("Volver", color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        if (movie.videoUrl.contains("drive.google.com")) {
                            val webIntent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                Uri.parse(movie.videoUrl)
                            )
                            val ctx = LocalContext.current
                            Button(
                                onClick = {
                                    ctx.startActivity(webIntent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A94FF))
                            ) {
                                Text("Abrir en Navegador / Drive")
                            }
                        }
                    }
                }
            }
        }

        // Amazon Prime styled Controls UI Overlay
        AnimatedVisibility(
            visible = showControls && errorMessage == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                // Top Bar with back button and Movie title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
                            .testTag("player_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = movie.title,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${movie.genre} • ${movie.year} • ${movie.duration}",
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )
                    }
                }

                // Center Seek Controls (skip back, Play/Pause, skip forward)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(48.dp)
                ) {
                    // Rewind 10s (re-purposed using Refresh icon)
                    IconButton(
                        onClick = {
                            videoViewRef?.let {
                                var target = it.currentPosition - 10000
                                if (target < 0) target = 0
                                it.seekTo(target)
                                currentPos = target.toLong()
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.Black.copy(alpha = 0.4f), shape = MaterialTheme.shapes.extraLarge)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Retroceder 10s",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Play / Pause (using Clear as pause/stop and PlayArrow as resume)
                    IconButton(
                        onClick = {
                            videoViewRef?.let {
                                if (it.isPlaying) {
                                    it.pause()
                                    isPlaying = false
                                } else {
                                    it.start()
                                    isPlaying = true
                                }
                            }
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color(0xFF1A94FF), shape = MaterialTheme.shapes.extraLarge)
                            .testTag("player_play_pause_button")
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Clear else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    // Forward 10s (re-purposed using PlayArrow icon)
                    IconButton(
                        onClick = {
                            videoViewRef?.let {
                                var target = it.currentPosition + 10000
                                if (target > duration) target = duration.toInt()
                                it.seekTo(target)
                                currentPos = target.toLong()
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.Black.copy(alpha = 0.4f), shape = MaterialTheme.shapes.extraLarge)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Adelantar 10s",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Bottom Timeline progress seeking
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Slider(
                        value = if (duration > 0) currentPos.toFloat() / duration.toFloat() else 0f,
                        onValueChange = { percent ->
                            videoViewRef?.let {
                                val target = (percent * duration).toInt()
                                it.seekTo(target)
                                currentPos = target.toLong()
                            }
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF1A94FF),
                            activeTrackColor = Color(0xFF1A94FF),
                            inactiveTrackColor = Color.LightGray.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentPos),
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                        Text(
                            text = formatTime(duration),
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// Format duration from MS to readable string "0:00"
private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}

// Convert sharing Google Drive URLs or direct WordPress URLs to working streams
fun cleanVideoUrl(url: String): String {
    var clean = url.trim()
    if (clean.contains("drive.google.com/file/d/")) {
        val parts = clean.split("/file/d/")
        if (parts.size > 1) {
            val idPart = parts[1].split("/")[0]
            return "https://drive.google.com/uc?export=download&id=$idPart"
        }
    } else if (clean.contains("drive.google.com/open?id=")) {
        val parts = clean.split("id=")
        if (parts.size > 1) {
            val idPart = parts[1].split("&")[0]
            return "https://drive.google.com/uc?export=download&id=$idPart"
        }
    }
    return clean
}
