package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.util.Log
import android.view.KeyEvent as AndroidKeyEvent
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.Movie
import com.example.ui.components.tvFocusable
import com.example.ui.viewmodel.MovieViewModel
import com.example.util.DeviceUtils
import com.example.util.GoogleDriveStreamResolver
import com.example.util.ResolvedStream
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * PrimePlex Native Video Player using ExoPlayer (Media3).
 * Streams video content directly like Plex, displaying raw video frames natively in ExoPlayer
 * without ever embedding or inserting Google Drive web players, iframes, or Google Play dialogs.
 */
@Composable
fun PlayerScreen(
    movieId: Int,
    viewModel: MovieViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val movieState = viewModel.allMovies.collectAsState().value
    val movie = movieState.find { it.id == movieId }

    // Lock orientation to Landscape for cinematic viewing on mobile; keep system landscape on TV
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val isTv = DeviceUtils.isTv(context)
        if (!isTv) {
            val previousOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            onDispose {
                activity?.requestedOrientation = previousOrientation
            }
        } else {
            onDispose { }
        }
    }

    BackHandler {
        onNavigateBack()
    }

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

    if (movie.videoUrl.isBlank()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF09111E))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF00A8E1),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Sin enlace de reproducción",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\"${movie.title}\" está en tu biblioteca con todos sus metadatos oficiales, pero no cuenta con un enlace de vídeo para reproducir.",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A8E1))
                ) {
                    Text("Regresar a la biblioteca", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    // Pure Native ExoPlayer Instance (No WebViews, No Google Drive player overlays)
    NativePlexExoPlayer(
        movie = movie,
        viewModel = viewModel,
        onNavigateBack = onNavigateBack
    )
}

/**
 * 100% Native Video Player Engine (ExoPlayer Media3).
 * Resolves the raw media stream and directly renders video frames to PlayerView.
 */
@OptIn(UnstableApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun NativePlexExoPlayer(
    movie: Movie,
    viewModel: MovieViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // Resolution and playback states
    var resolvedStream by remember { mutableStateOf<ResolvedStream?>(null) }
    var isResolving by remember { mutableStateOf(true) }
    var streamError by remember { mutableStateOf<String?>(null) }
    var retryCount by remember { mutableIntStateOf(0) }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPosMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var isBuffering by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }

    // Remote navigation focus
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Create and configure ExoPlayer with browser User-Agent and cross-protocol support
    val exoPlayer = remember {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(25000)
            .setReadTimeoutMs(25000)

        val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
    }

    // Auto-hide controls overlay after 4 seconds of playback
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    // Resolve Google Drive or direct stream URL and pass to ExoPlayer
    LaunchedEffect(movie.videoUrl, retryCount) {
        isResolving = true
        isBuffering = true
        streamError = null
        try {
            val resolved = GoogleDriveStreamResolver.resolveStream(movie.videoUrl)
            resolvedStream = resolved
            isResolving = false

            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(25000)
                .setReadTimeoutMs(25000)

            val headers = mutableMapOf<String, String>()
            headers["Accept"] = "*/*"
            if (!resolved.cookieHeader.isNullOrBlank()) {
                headers["Cookie"] = resolved.cookieHeader
            }
            httpDataSourceFactory.setDefaultRequestProperties(headers)

            val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory)
            val mediaItem = MediaItem.fromUri(Uri.parse(resolved.streamUrl))
            val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)

            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true

            // Resume saved playback progress
            viewModel.viewModelScope.launch {
                val savedProgress = viewModel.getMoviePlaybackProgress(movie.id).first()
                if (savedProgress > 0) {
                    exoPlayer.seekTo(savedProgress)
                    currentPosMs = savedProgress
                }
            }
        } catch (e: Exception) {
            Log.e("NativePlexExoPlayer", "Error preparing stream: ${e.message}", e)
            streamError = "Error al resolver la transmisión de vídeo: ${e.message}"
            isResolving = false
            isBuffering = false
        }
    }

    // Attach ExoPlayer State Listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        isBuffering = true
                    }
                    Player.STATE_READY -> {
                        isBuffering = false
                        val dur = exoPlayer.duration
                        if (dur > 0) durationMs = dur
                    }
                    Player.STATE_ENDED -> {
                        isPlaying = false
                        viewModel.clearPlaybackProgress(movie.id)
                        onNavigateBack()
                    }
                    Player.STATE_IDLE -> {
                        isBuffering = false
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) isBuffering = false
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("NativePlexExoPlayer", "ExoPlayer playback error: ${error.errorCodeName} - ${error.message}", error)
                isBuffering = false
                streamError = "No se pudo reproducir este archivo de vídeo directamente en ExoPlayer."
            }
        }

        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Periodic position updater and progress saver
    LaunchedEffect(exoPlayer, isPlaying) {
        while (true) {
            if (exoPlayer.playbackState == Player.STATE_READY) {
                val pos = exoPlayer.currentPosition
                val dur = exoPlayer.duration
                if (pos >= 0) currentPosMs = pos
                if (dur > 0) durationMs = dur
                if (pos > 0 && dur > 0 && isPlaying) {
                    viewModel.updatePlaybackProgress(movie.id, pos, dur)
                }
            }
            delay(1000)
        }
    }

    // Action Helpers
    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
        showControls = true
    }

    fun seekRelative(deltaMs: Long) {
        val target = (exoPlayer.currentPosition + deltaMs).coerceIn(0L, exoPlayer.duration.coerceAtLeast(0L))
        exoPlayer.seekTo(target)
        currentPosMs = target
        showControls = true
    }

    fun seekAbsolute(targetMs: Long) {
        exoPlayer.seekTo(targetMs)
        currentPosMs = targetMs
        showControls = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("custom_player_container")
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        AndroidKeyEvent.KEYCODE_DPAD_CENTER,
                        AndroidKeyEvent.KEYCODE_ENTER,
                        AndroidKeyEvent.KEYCODE_NUMPAD_ENTER,
                        AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            togglePlayPause()
                            true
                        }
                        AndroidKeyEvent.KEYCODE_MEDIA_PLAY -> {
                            if (!isPlaying) togglePlayPause()
                            true
                        }
                        AndroidKeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            if (isPlaying) togglePlayPause()
                            true
                        }
                        AndroidKeyEvent.KEYCODE_DPAD_LEFT,
                        AndroidKeyEvent.KEYCODE_MEDIA_REWIND -> {
                            seekRelative(-10000L)
                            true
                        }
                        AndroidKeyEvent.KEYCODE_DPAD_RIGHT,
                        AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                            seekRelative(10000L)
                            true
                        }
                        AndroidKeyEvent.KEYCODE_DPAD_UP,
                        AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                            showControls = !showControls
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
            }
    ) {
        // Native Video View: Renders video image directly from the stream (Plex style)
        // No Google Drive web players, no iframes, no third-party controls
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // Use our Prime Video Compose overlay
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setBackgroundColor(android.graphics.Color.BLACK)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading & Buffering Spinner
        if (isBuffering || isResolving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = Color(0xFF00A8E1),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (isResolving) "Conectando stream de vídeo..." else "Cargando reproductor...",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Reproducción directa nativa PrimePlex",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Native Plex Error Overlay (if stream cannot be decoded)
        if (streamError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFF00A8E1),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No se pudo reproducir el vídeo",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Asegúrate de que en Google Drive el enlace esté configurado con acceso público: \"Cualquier persona con el enlace\" (Lector) para permitir la transmisión directa a la aplicación sin restricciones.",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = {
                                retryCount++
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A8E1)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reintentar", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onNavigateBack,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Volver a la biblioteca", color = Color.White)
                        }
                    }
                }
            }
        }

        // ========================================================
        // 100% CUSTOM PRIME VIDEO / PLEX CONTROLS OVERLAY
        // ========================================================
        AnimatedVisibility(
            visible = showControls && streamError == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.9f)
                            )
                        )
                    )
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(40.dp)
                                .tvFocusable(shape = RoundedCornerShape(20.dp), focusedScale = 1.15f)
                                .testTag("custom_player_back_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = movie.title,
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(3.dp),
                                    color = Color(0xFF00A8E1)
                                ) {
                                    Text(
                                        text = "STREAMING DIRECTO",
                                        color = Color.Black,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                if (movie.imdbRating.isNotBlank()) {
                                    Text(
                                        text = "IMDb ${movie.imdbRating} ★",
                                        color = Color(0xFFF5C518),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = " • ",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = "${movie.year} • ${movie.genre}",
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Center Play / Pause & 10s Skip Buttons
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(40.dp)
                ) {
                    // Rewind 10s
                    IconButton(
                        onClick = { seekRelative(-10000L) },
                        modifier = Modifier
                            .size(52.dp)
                            .tvFocusable(shape = RoundedCornerShape(26.dp), focusedScale = 1.15f)
                            .testTag("custom_player_rewind_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Retroceder 10s",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                text = "10",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Main Center Play / Pause
                    IconButton(
                        onClick = { togglePlayPause() },
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(36.dp)
                            )
                            .tvFocusable(
                                shape = RoundedCornerShape(36.dp),
                                focusedScale = 1.15f,
                                focusedBorderColor = Color(0xFF00A8E1)
                            )
                            .testTag("custom_player_play_pause_button")
                    ) {
                        if (isPlaying) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(6.dp)
                                        .height(26.dp)
                                        .background(Color.White, RoundedCornerShape(2.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .width(6.dp)
                                        .height(26.dp)
                                        .background(Color.White, RoundedCornerShape(2.dp))
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Reproducir",
                                tint = Color.White,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }

                    // Forward 10s
                    IconButton(
                        onClick = { seekRelative(10000L) },
                        modifier = Modifier
                            .size(52.dp)
                            .tvFocusable(shape = RoundedCornerShape(26.dp), focusedScale = 1.15f)
                            .testTag("custom_player_forward_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Avanzar 10s",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                text = "+10",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Bottom Timeline, Progress Scrubber & Duration
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    val progressFraction = if (durationMs > 0) {
                        (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Slider(
                        value = progressFraction,
                        onValueChange = { fraction ->
                            val target = (fraction * durationMs).toLong()
                            seekAbsolute(target)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00A8E1),
                            activeTrackColor = Color(0xFF00A8E1),
                            inactiveTrackColor = Color.LightGray.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvFocusable(shape = RoundedCornerShape(4.dp), focusedScale = 1.02f)
                            .testTag("custom_player_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(currentPosMs),
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (durationMs > 0) formatTime(durationMs) else "--:--",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Formats milliseconds to mm:ss or hh:mm:ss.
 */
private fun formatTime(millis: Long): String {
    if (millis <= 0) return "0:00"
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}
