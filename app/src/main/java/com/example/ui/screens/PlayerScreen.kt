package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.KeyEvent as AndroidKeyEvent
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.VideoView
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewModelScope
import com.example.data.model.Movie
import com.example.ui.components.tvFocusable
import com.example.ui.viewmodel.MovieViewModel
import com.example.util.DeviceUtils
import com.example.util.GoogleDriveStreamResolver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * PrimePlex Custom Video Player.
 * Delivers an immediate playback experience for all video streams,
 * including public Google Drive files, with a 100% custom Prime Video player
 * interface (no Google Drive web controls and no Google Play dialogs).
 */
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

    // Lock orientation to Landscape for cinematic viewing on mobile; on TV keep system landscape
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
                    text = "\"${movie.title}\" está guardada en tu biblioteca con todos sus metadatos oficiales, pero no cuenta con un enlace de vídeo para reproducir.",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    lineHeight = 18.sp
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

    // Unified Custom Prime Player
    PrimeCustomPlayer(
        movie = movie,
        viewModel = viewModel,
        onNavigateBack = onNavigateBack
    )
}

/**
 * Unified Custom Player Engine for PrimePlex.
 * Directly plays Google Drive, MP4, HLS, or WordPress videos with custom Prime Video controls.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun PrimeCustomPlayer(
    movie: Movie,
    viewModel: MovieViewModel,
    onNavigateBack: () -> Unit
) {
    val isGoogleDrive = remember(movie.videoUrl) { GoogleDriveStreamResolver.isGoogleDriveUrl(movie.videoUrl) }
    var resolvedDirectUrl by remember { mutableStateOf<String?>(null) }
    var isResolving by remember { mutableStateOf(isGoogleDrive) }

    // Player playback state
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var isBuffering by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var useWebEngineFallback by remember { mutableStateOf(false) }

    // References to underlying video engines
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Focus requester for TV D-Pad remote control
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Auto-hide controls overlay after 4 seconds of playback
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    // Step 1: Pre-resolve direct stream URL for Google Drive links
    LaunchedEffect(movie.videoUrl) {
        if (isGoogleDrive) {
            isResolving = true
            val direct = GoogleDriveStreamResolver.resolveDirectStreamUrl(movie.videoUrl)
            resolvedDirectUrl = direct
            isResolving = false
        } else {
            resolvedDirectUrl = movie.videoUrl.trim()
            isResolving = false
        }
    }

    // Periodically save playback progress in Room database
    LaunchedEffect(isPlaying, currentPosMs) {
        if (currentPosMs > 0 && durationMs > 0) {
            viewModel.updatePlaybackProgress(movie.id, currentPosMs, durationMs)
        }
    }

    // Action Helpers
    fun togglePlayPause() {
        if (useWebEngineFallback) {
            val script = if (isPlaying) {
                "var v = document.querySelector('video'); if (v) { v.pause(); }"
            } else {
                "var v = document.querySelector('video'); if (v) { v.play(); }"
            }
            webViewRef?.evaluateJavascript(script, null)
            isPlaying = !isPlaying
        } else {
            videoViewRef?.let {
                if (isPlaying) {
                    it.pause()
                    isPlaying = false
                } else {
                    it.start()
                    isPlaying = true
                }
            }
        }
        showControls = true
    }

    fun seekRelative(deltaMs: Long) {
        val target = (currentPosMs + deltaMs).coerceIn(0L, if (durationMs > 0) durationMs else Long.MAX_VALUE)
        if (useWebEngineFallback) {
            val targetSec = target / 1000.0
            webViewRef?.evaluateJavascript("var v = document.querySelector('video'); if (v) { v.currentTime = $targetSec; }", null)
            currentPosMs = target
        } else {
            videoViewRef?.seekTo(target.toInt())
            currentPosMs = target
        }
        showControls = true
    }

    fun seekAbsolute(targetMs: Long) {
        if (useWebEngineFallback) {
            val targetSec = targetMs / 1000.0
            webViewRef?.evaluateJavascript("var v = document.querySelector('video'); if (v) { v.currentTime = $targetSec; }", null)
            currentPosMs = targetMs
        } else {
            videoViewRef?.seekTo(targetMs.toInt())
            currentPosMs = targetMs
        }
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
        // Underlying Video Engine
        if (!useWebEngineFallback && resolvedDirectUrl != null && !isResolving) {
            // Engine A: Hardware Native Android VideoView
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        val uri = Uri.parse(resolvedDirectUrl)
                        setVideoURI(uri)
                        setOnPreparedListener { mp ->
                            isBuffering = false
                            durationMs = mp.duration.toLong()
                            mp.start()
                            isPlaying = true

                            // Resume saved position if present
                            viewModel.viewModelScope.launch {
                                val saved = viewModel.getMoviePlaybackProgress(movie.id).first()
                                if (saved > 0 && saved < durationMs - 5000) {
                                    seekTo(saved.toInt())
                                    currentPosMs = saved
                                }
                            }
                        }
                        setOnErrorListener { _, what, extra ->
                            Log.w("PrimeCustomPlayer", "VideoView playback error ($what, $extra). Switching smoothly to headless stream engine.")
                            // Fallback smoothly to custom headless web engine (never shows Google Play)
                            useWebEngineFallback = true
                            true
                        }
                        setOnCompletionListener {
                            isPlaying = false
                            viewModel.clearPlaybackProgress(movie.id)
                            onNavigateBack()
                        }
                    }
                },
                update = { view ->
                    videoViewRef = view
                },
                modifier = Modifier.fillMaxSize()
            )

            // Periodic progress tracking for Native VideoView
            LaunchedEffect(isPlaying, videoViewRef) {
                while (isPlaying && videoViewRef != null && !useWebEngineFallback) {
                    val pos = videoViewRef?.currentPosition?.toLong() ?: 0L
                    val dur = videoViewRef?.duration?.toLong() ?: 0L
                    if (pos > 0) currentPosMs = pos
                    if (dur > 0) durationMs = dur
                    delay(1000)
                }
            }
        } else if (useWebEngineFallback || (isGoogleDrive && resolvedDirectUrl == null && !isResolving)) {
            // Engine B: Headless HTML5 Media Engine
            // Strips all Google Drive controls, branding, and "Open in Google Play" buttons with CSS/JS injection
            val drivePreviewUrl = remember(movie.videoUrl) {
                val fileId = GoogleDriveStreamResolver.extractGoogleDriveFileId(movie.videoUrl)
                if (fileId != null) "https://drive.google.com/file/d/$fileId/preview" else movie.videoUrl.trim()
            }

            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(android.graphics.Color.BLACK)

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            allowFileAccess = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                        }

                        // Javascript bridge to receive video playback updates
                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onProgress(posSeconds: Float, durSeconds: Float) {
                                currentPosMs = (posSeconds * 1000).toLong()
                                if (durSeconds > 0) durationMs = (durSeconds * 1000).toLong()
                                isBuffering = false
                            }

                            @JavascriptInterface
                            fun onPlayState(playing: Boolean) {
                                isPlaying = playing
                                isBuffering = false
                            }
                        }, "PrimeBridge")

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isBuffering = false
                                isPlaying = true

                                // CSS and JS injection to:
                                // 1. Hide all Google Drive UI, banners, popout buttons, and Play Store links.
                                // 2. Make video element fill the screen completely.
                                // 3. Auto-play video immediately.
                                // 4. Stream position back to our custom Prime Video controls overlay.
                                val injectionJs = """
                                    (function() {
                                        var style = document.createElement('style');
                                        style.innerHTML = `
                                            .drive-viewer-toolstrip, .drive-viewer-popout-button, .ytp-chrome-top, .ytp-chrome-bottom,
                                            .gb_a, .drive-viewer-navigation-button, .drive-viewer-action-bar, .goog-inline-block,
                                            .ndfHFb-c4YZDc-Wrql6b, a[href*='play.google.com'], a[href*='drive.google.com'],
                                            .drive-viewer-toolstrip-inner, .drive-viewer-popout {
                                                display: none !important;
                                                opacity: 0 !important;
                                                visibility: hidden !important;
                                                pointer-events: none !important;
                                            }
                                            body, html {
                                                background-color: #000 !important;
                                                overflow: hidden !important;
                                                margin: 0 !important;
                                                padding: 0 !important;
                                            }
                                            video {
                                                position: fixed !important;
                                                top: 0 !important;
                                                left: 0 !important;
                                                width: 100vw !important;
                                                height: 100vh !important;
                                                object-fit: contain !important;
                                                z-index: 1000 !important;
                                                background: #000 !important;
                                            }
                                        `;
                                        document.head.appendChild(style);

                                        function bindVideo() {
                                            var v = document.querySelector('video');
                                            if (v) {
                                                v.autoplay = true;
                                                v.play().catch(function(){});
                                                v.addEventListener('timeupdate', function() {
                                                    if (window.PrimeBridge) {
                                                        window.PrimeBridge.onProgress(v.currentTime, v.duration || 0);
                                                    }
                                                });
                                                v.addEventListener('play', function() {
                                                    if (window.PrimeBridge) window.PrimeBridge.onPlayState(true);
                                                });
                                                v.addEventListener('pause', function() {
                                                    if (window.PrimeBridge) window.PrimeBridge.onPlayState(false);
                                                });
                                            } else {
                                                setTimeout(bindVideo, 400);
                                            }
                                        }
                                        bindVideo();
                                    })();
                                """.trimIndent()

                                view?.evaluateJavascript(injectionJs, null)
                            }
                        }

                        webChromeClient = object : WebChromeClient() {}
                        loadUrl(drivePreviewUrl)
                        webViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Buffering Indicator
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
                        text = "Iniciando reproducción directa...",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Conectando reproductor personalizado PrimePlex",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // ========================================================
        // 100% CUSTOM PRIME VIDEO CONTROLS OVERLAY
        // Never shows Google Drive branding or Google Play buttons
        // ========================================================
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.8f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
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
                                        text = "HD • PrimePlex",
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
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Retroceder 10s",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
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
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausa" else "Reproducir",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    // Forward 10s
                    IconButton(
                        onClick = { seekRelative(10000L) },
                        modifier = Modifier
                            .size(52.dp)
                            .tvFocusable(shape = RoundedCornerShape(26.dp), focusedScale = 1.15f)
                            .testTag("custom_player_forward_button")
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Avanzar 10s",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
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
                            text = formatTime(durationMs),
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
 * Formats milliseconds to a human-readable mm:ss or hh:mm:ss format.
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
