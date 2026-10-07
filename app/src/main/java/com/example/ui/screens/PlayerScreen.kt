package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent as AndroidKeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
 * Extends display edge-to-edge behind notches and cutouts, and completely hides
 * the system status bar, bottom navigation buttons (arrows, home, recents/pill)
 * so that video playback occupies 100% of the screen.
 */
fun applyImmersivePlayerFullscreen(window: android.view.Window?) {
    if (window == null) return
    try {
        // 1. Extend behind notch / cutout so video covers 100% of the display edge-to-edge
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val attrs = window.attributes
            attrs.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            window.attributes = attrs
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val attrs = window.attributes
            attrs.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = attrs
        }

        // 2. Hide system bars (bottom navigation arrows/pill, top status bar, menu)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        // 3. Keep display powered on while playing and add full screen flags
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        // 4. Maximum sticky immersive flags for all Android versions
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    } catch (e: Exception) {
        Log.e("PlayerFullscreen", "Error applying immersive mode: ${e.message}")
    }
}

/**
 * PrimePlex Native Video Player using ExoPlayer (Media3).
 * Streams video content directly like Plex, displaying raw video frames natively in ExoPlayer
 * without ever embedding or inserting Google Drive web players, iframes, or Google Play dialogs.
 */
@Composable
fun PlayerScreen(
    movieId: Int,
    episodeIndex: Int = -1,
    viewModel: MovieViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val movieState = viewModel.allMovies.collectAsState().value
    val movie = movieState.find { it.id == movieId }

    val activity = context as? Activity
    val window = activity?.window
    val isTv = remember { DeviceUtils.isTv(context) }

    // Lock orientation to Landscape & enable 100% Immersive Fullscreen hiding notch and system bars
    DisposableEffect(Unit) {
        val previousOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        val originalCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window?.attributes?.layoutInDisplayCutoutMode
        } else null

        if (!isTv) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }

        applyImmersivePlayerFullscreen(window)

        // Re-apply immediately when window regains focus (e.g. after transient notification)
        val focusListener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus) applyImmersivePlayerFullscreen(window)
        }
        window?.decorView?.viewTreeObserver?.addOnWindowFocusChangeListener(focusListener)

        @Suppress("DEPRECATION")
        window?.decorView?.setOnSystemUiVisibilityChangeListener { visibility ->
            if ((visibility and View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                applyImmersivePlayerFullscreen(window)
            }
        }

        onDispose {
            window?.decorView?.viewTreeObserver?.removeOnWindowFocusChangeListener(focusListener)
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, true)
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && originalCutoutMode != null) {
                    val attrs = window.attributes
                    attrs.layoutInDisplayCutoutMode = originalCutoutMode
                    window.attributes = attrs
                }

                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
            if (!isTv) {
                activity?.requestedOrientation = previousOrientation
            }
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

    val episodes = remember(movie.episodesJson) { movie.getEpisodes() }
    val isSeries = movie.category.equals("Series", ignoreCase = true)

    // Determine target episode and target stream URL
    val targetEpisode = if (isSeries && episodes.isNotEmpty()) {
        if (episodeIndex in episodes.indices) {
            episodes[episodeIndex]
        } else {
            // Find first episode with video URL or fallback to first episode
            episodes.firstOrNull { it.videoUrl.isNotBlank() } ?: episodes.first()
        }
    } else null

    val resolvedEpisodeIndex = if (targetEpisode != null) episodes.indexOf(targetEpisode) else -1

    val activeVideoUrl = if (targetEpisode != null) {
        targetEpisode.videoUrl.ifBlank { movie.videoUrl }
    } else {
        movie.videoUrl
    }

    if (activeVideoUrl.isBlank()) {
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
                    text = if (targetEpisode != null)
                        "El capítulo \"T${targetEpisode.seasonNumber}:E${targetEpisode.episodeNumber} ${targetEpisode.title}\" no cuenta con un enlace de vídeo para reproducir."
                    else
                        "\"${movie.title}\" está en tu biblioteca con todos sus metadatos oficiales, pero no cuenta con un enlace de vídeo para reproducir.",
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
        activeVideoUrl = activeVideoUrl,
        episodeIndex = resolvedEpisodeIndex,
        episodeTitle = targetEpisode?.title ?: "",
        seasonNumber = targetEpisode?.seasonNumber ?: 1,
        episodeNumber = targetEpisode?.episodeNumber ?: 1,
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
    activeVideoUrl: String,
    episodeIndex: Int,
    episodeTitle: String,
    seasonNumber: Int,
    episodeNumber: Int,
    viewModel: MovieViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val window = activity?.window

    var currentResizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_ZOOM) }
    var showCastDialog by remember { mutableStateOf(false) }
    var isCasting by remember { mutableStateOf(false) }

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

    var targetResumeMs by remember { mutableStateOf(0L) }
    var initialSeekDone by remember { mutableStateOf(false) }
    var resumeNotification by remember { mutableStateOf<String?>(null) }

    // Auto-dismiss resume notification after 5 seconds
    LaunchedEffect(resumeNotification) {
        if (resumeNotification != null) {
            delay(5000)
            resumeNotification = null
        }
    }

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

    // Auto-hide controls overlay after 3.5 seconds of playback and enforce 100% immersive fullscreen
    // hiding notch cutout, status bar, and bottom navigation bar (back arrows, home, recents/menu)
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(3500)
            showControls = false
        }
        applyImmersivePlayerFullscreen(window)
    }

    // Resolve Google Drive or direct stream URL and pass to ExoPlayer
    LaunchedEffect(activeVideoUrl, retryCount) {
        isResolving = true
        isBuffering = true
        streamError = null
        try {
            val resolved = GoogleDriveStreamResolver.resolveStream(activeVideoUrl)
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

            // Retrieve exact saved progress in ms
            val savedProgress = viewModel.getMoviePlaybackProgress(movie.id).first()
            targetResumeMs = savedProgress

            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
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

                        // Resume exactly by minute and second where left off
                        if (!initialSeekDone && targetResumeMs > 1500L) {
                            initialSeekDone = true
                            if (dur <= 0 || targetResumeMs < dur - 4000L) {
                                exoPlayer.seekTo(targetResumeMs)
                                currentPosMs = targetResumeMs
                                resumeNotification = "Continuando en el minuto ${formatTime(targetResumeMs)}"
                            }
                        }
                    }
                    Player.STATE_ENDED -> {
                        isPlaying = false
                        val finalDur = if (exoPlayer.duration > 0) exoPlayer.duration else durationMs
                        viewModel.markAsWatched(movie.id, finalDur)
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
            val finalPos = exoPlayer.currentPosition
            val finalDur = exoPlayer.duration
            if (finalPos > 1000L && finalDur > 0) {
                viewModel.updatePlaybackProgress(
                    movieId = movie.id,
                    progressMs = finalPos,
                    durationMs = finalDur,
                    episodeIndex = episodeIndex,
                    episodeNumber = episodeNumber,
                    seasonNumber = seasonNumber,
                    episodeTitle = episodeTitle
                )
            }
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
                    viewModel.updatePlaybackProgress(
                        movieId = movie.id,
                        progressMs = pos,
                        durationMs = dur,
                        episodeIndex = episodeIndex,
                        episodeNumber = episodeNumber,
                        seasonNumber = seasonNumber,
                        episodeTitle = episodeTitle
                    )
                }
            }
            delay(1000)
        }
    }

    // Action Helpers
    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            val pos = exoPlayer.currentPosition
            val dur = exoPlayer.duration
            if (pos > 0 && dur > 0) {
                viewModel.updatePlaybackProgress(
                    movieId = movie.id,
                    progressMs = pos,
                    durationMs = dur,
                    episodeIndex = episodeIndex,
                    episodeNumber = episodeNumber,
                    seasonNumber = seasonNumber,
                    episodeTitle = episodeTitle
                )
            }
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
                applyImmersivePlayerFullscreen(window)
            }
    ) {
        // Native Video View: Renders video image directly from the stream (Plex style)
        // No Google Drive web players, no iframes, no third-party controls
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // Use our Prime Video Compose overlay
                    resizeMode = currentResizeMode
                    setBackgroundColor(android.graphics.Color.BLACK)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.resizeMode = currentResizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Active Casting to Chromecast Overlay
        if (isCasting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    ChromecastConnectedIcon(
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFF00A8E1)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Transmitiendo a pantalla",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (episodeIndex >= 0 && episodeTitle.isNotBlank())
                            "${movie.title} • T${seasonNumber}:E${episodeNumber} $episodeTitle"
                        else
                            movie.title,
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = { togglePlayPause() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A8E1)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isPlaying) "Pausar en TV" else "Reanudar en TV", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { isCasting = false },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Detener transmisión", color = Color.White)
                        }
                    }
                }
            }
        }

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

        // Floating notification pill showing exact minute & second resumed
        AnimatedVisibility(
            visible = resumeNotification != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F1E36).copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00A8E1)),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF00A8E1),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = resumeNotification ?: "",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    TextButton(
                        onClick = {
                            exoPlayer.seekTo(0L)
                            currentPosMs = 0L
                            viewModel.updatePlaybackProgress(
                                movieId = movie.id,
                                progressMs = 0L,
                                durationMs = durationMs,
                                episodeIndex = episodeIndex,
                                episodeNumber = episodeNumber,
                                seasonNumber = seasonNumber,
                                episodeTitle = episodeTitle
                            )
                            resumeNotification = null
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Ver desde 0:00", color = Color(0xFF00A8E1), fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                    text = if (episodeIndex >= 0 && episodeTitle.isNotBlank())
                                        "${movie.title} • T${seasonNumber}:E${episodeNumber} $episodeTitle"
                                    else if (episodeIndex >= 0)
                                        "${movie.title} • T${seasonNumber}:E${episodeNumber}"
                                    else
                                        movie.title,
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
                                        text = if (episodeIndex >= 0) "EPISODIO HD" else "STREAMING DIRECTO",
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

                    // Top Bar Actions: Aspect Ratio (Notch / Fullscreen), Chromecast and Restart
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Aspect Ratio / Notch Zoom Mode Toggle
                        IconButton(
                            onClick = {
                                currentResizeMode = when (currentResizeMode) {
                                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> {
                                        Toast.makeText(context, "Modo: Ajustar al centro (Formato original)", Toast.LENGTH_SHORT).show()
                                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT -> {
                                        Toast.makeText(context, "Modo: Estirar vídeo (Fill)", Toast.LENGTH_SHORT).show()
                                        AspectRatioFrameLayout.RESIZE_MODE_FILL
                                    }
                                    else -> {
                                        Toast.makeText(context, "Modo: Pantalla completa (Sin notch ni bordes)", Toast.LENGTH_SHORT).show()
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                .tvFocusable(shape = CircleShape, focusedScale = 1.15f)
                                .testTag("player_aspect_ratio_button")
                        ) {
                            Icon(
                                Icons.Default.AspectRatio,
                                contentDescription = "Modo de pantalla completa y notch",
                                tint = if (currentResizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) Color(0xFF00A8E1) else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Chromecast Button
                        IconButton(
                            onClick = { showCastDialog = true },
                            modifier = Modifier
                                .size(38.dp)
                                .background(if (isCasting) Color(0xFF00A8E1).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.15f), CircleShape)
                                .tvFocusable(shape = CircleShape, focusedScale = 1.15f)
                                .testTag("player_chromecast_button")
                        ) {
                            if (isCasting) {
                                ChromecastConnectedIcon(modifier = Modifier.size(20.dp), tint = Color(0xFF00A8E1))
                            } else {
                                ChromecastIcon(modifier = Modifier.size(20.dp), tint = Color.White)
                            }
                        }

                        // Option to restart from 0:00 if playback is progressed
                        if (currentPosMs > 5000L) {
                            OutlinedButton(
                                onClick = {
                                    exoPlayer.seekTo(0L)
                                    currentPosMs = 0L
                                    viewModel.updatePlaybackProgress(
                                        movieId = movie.id,
                                        progressMs = 0L,
                                        durationMs = durationMs,
                                        episodeIndex = episodeIndex,
                                        episodeNumber = episodeNumber,
                                        seasonNumber = seasonNumber,
                                        episodeTitle = episodeTitle
                                    )
                                },
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .tvFocusable(shape = RoundedCornerShape(16.dp), focusedScale = 1.05f)
                                    .testTag("player_restart_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF00A8E1))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Desde 0:00", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

        // Chromecast Streaming Dialog
        if (showCastDialog) {
            ChromecastDialog(
                title = if (episodeIndex >= 0 && episodeTitle.isNotBlank())
                    "${movie.title} • T${seasonNumber}:E${episodeNumber} $episodeTitle"
                else
                    movie.title,
                streamUrl = resolvedStream?.streamUrl ?: activeVideoUrl,
                isCasting = isCasting,
                onToggleCastState = { isCasting = !isCasting },
                onDismiss = { showCastDialog = false }
            )
        }
    }
}

/**
 * Chromecast Casting Dialog for streaming to screens, Smart TVs and DLNA/Google Cast receivers.
 */
@Composable
fun ChromecastDialog(
    title: String,
    streamUrl: String,
    isCasting: Boolean,
    onToggleCastState: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F1E36),
        titleContentColor = Color.White,
        textContentColor = Color.LightGray,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChromecastConnectedIcon(modifier = Modifier.size(28.dp), tint = Color(0xFF00A8E1))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Enviar a pantalla", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Transmite la reproducción directamente a tu Chromecast, Google TV, Android TV o Smart TV en la misma red Wi-Fi.",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

                // Option 1: Native System Cast Discovery
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E2E4A),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            var launched = false
                            try {
                                val castIntent = Intent(Settings.ACTION_CAST_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(castIntent)
                                launched = true
                            } catch (_: Exception) {}
                            if (!launched) {
                                try {
                                    val wifiDisplayIntent = Intent("android.settings.WIFI_DISPLAY_SETTINGS").apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(wifiDisplayIntent)
                                    launched = true
                                } catch (_: Exception) {}
                            }
                            if (launched) {
                                Toast.makeText(context, "Buscando dispositivos Chromecast y pantallas...", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Abre el panel rápido y pulsa 'Transmitir pantalla / Smart View'", Toast.LENGTH_LONG).show()
                            }
                            onDismiss()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Tv, contentDescription = null, tint = Color(0xFF00A8E1), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Chromecast / Pantalla inalámbrica", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Abre el selector de Google Cast y Smart View", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }

                // Option 2: External Casting App (Web Video Caster / VLC / BubbleUPnP)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E2E4A),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (streamUrl.isNotBlank()) {
                                try {
                                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(Uri.parse(streamUrl), "video/*")
                                        putExtra("title", title)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    val chooser = Intent.createChooser(viewIntent, "Transmitir con app externa (Web Video Caster / VLC):")
                                    context.startActivity(chooser)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "No se encontró app de transmisión externa compatible", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Conectando stream de vídeo...", Toast.LENGTH_SHORT).show()
                            }
                            onDismiss()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.SendToMobile, contentDescription = null, tint = Color(0xFFFF9900), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Transmitir con Web Video Caster / VLC", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Envío directo a Chromecast, DLNA y Smart TV", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }

                // Option 3: Copy direct stream link
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E2E4A),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (streamUrl.isNotBlank()) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip = ClipData.newPlainText("Streaming URL", streamUrl)
                                clipboard?.setPrimaryClip(clip)
                                Toast.makeText(context, "Enlace copiado para Smart TV o navegador", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Enlace no disponible todavía", Toast.LENGTH_SHORT).show()
                            }
                            onDismiss()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Copiar enlace de transmisión", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Para reproducir en navegador de TV o consola", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }

                // Toggle Cast Active State
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isCasting) "Transmitiendo a TV (Activo)" else "Modo transmisión",
                        color = if (isCasting) Color(0xFF00A8E1) else Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Switch(
                        checked = isCasting,
                        onCheckedChange = {
                            onToggleCastState()
                            onDismiss()
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00A8E1))
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = Color(0xFF00A8E1))
            }
        }
    )
}

/**
 * Custom vector icon representing the official Chromecast screen and broadcast waves.
 */
@Composable
fun ChromecastIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        // TV screen frame
        val framePath = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.88f, h * 0.15f)
            lineTo(w * 0.12f, h * 0.15f)
            cubicTo(w * 0.08f, h * 0.15f, w * 0.05f, h * 0.18f, w * 0.05f, h * 0.22f)
            lineTo(w * 0.05f, h * 0.35f)
            lineTo(w * 0.14f, h * 0.35f)
            lineTo(w * 0.14f, h * 0.23f)
            lineTo(w * 0.86f, h * 0.23f)
            lineTo(w * 0.86f, h * 0.77f)
            lineTo(w * 0.60f, h * 0.77f)
            lineTo(w * 0.60f, h * 0.85f)
            lineTo(w * 0.88f, h * 0.85f)
            cubicTo(w * 0.92f, h * 0.85f, w * 0.95f, h * 0.82f, w * 0.95f, h * 0.77f)
            lineTo(w * 0.95f, h * 0.22f)
            cubicTo(w * 0.95f, h * 0.18f, w * 0.92f, h * 0.15f, w * 0.88f, h * 0.15f)
            close()
        }
        drawPath(framePath, color = tint)

        // Wave 1 (dot)
        drawCircle(color = tint, radius = w * 0.05f, center = androidx.compose.ui.geometry.Offset(w * 0.12f, h * 0.82f))

        // Wave 2 (inner arc)
        drawArc(
            color = tint,
            startAngle = -90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.05f - w * 0.18f, h * 0.85f - h * 0.18f),
            size = androidx.compose.ui.geometry.Size(w * 0.36f, h * 0.36f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.07f)
        )

        // Wave 3 (outer arc)
        drawArc(
            color = tint,
            startAngle = -90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.05f - w * 0.35f, h * 0.85f - h * 0.35f),
            size = androidx.compose.ui.geometry.Size(w * 0.70f, h * 0.70f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.07f)
        )
    }
}

@Composable
fun ChromecastConnectedIcon(modifier: Modifier = Modifier, tint: Color = Color(0xFF00A8E1)) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        ChromecastIcon(modifier = Modifier.fillMaxSize(), tint = tint)
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
