package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

    val isDriveLink = remember(movie.videoUrl) { isGoogleDriveUrl(movie.videoUrl) }
    var useWebPlayer by remember(movie.videoUrl) { mutableStateOf(isDriveLink) }

    if (useWebPlayer) {
        GoogleDriveWebPlayer(
            movie = movie,
            onNavigateBack = onNavigateBack,
            onSwitchToNative = { useWebPlayer = false }
        )
    } else {
        NativeVideoPlayer(
            movie = movie,
            viewModel = viewModel,
            onNavigateBack = onNavigateBack,
            onSwitchToWebPlayer = { useWebPlayer = true }
        )
    }
}

/**
 * Dedicated Hardware-Accelerated Web Stream Player for Google Drive videos.
 * Uses Google Drive's official HTML5 streaming embed player (/preview), which supports
 * on-the-fly video transcoding, audio, subtitles, seek bar, and resolution switching.
 */
@Composable
private fun GoogleDriveWebPlayer(
    movie: Movie,
    onNavigateBack: () -> Unit,
    onSwitchToNative: () -> Unit
) {
    val context = LocalContext.current
    val previewUrl = remember(movie.videoUrl) { getGoogleDrivePreviewUrl(movie.videoUrl) }
    var isPageLoading by remember { mutableStateOf(true) }
    var showOverlayControls by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var customViewContainer by remember { mutableStateOf<View?>(null) }

    // Auto-hide top overlay controls after 4 seconds
    LaunchedEffect(showOverlayControls) {
        if (showOverlayControls) {
            delay(4500)
            showOverlayControls = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("google_drive_player_container")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showOverlayControls = !showOverlayControls
            }
    ) {
        // Embedded Android WebView for Google Drive HTML5 Player
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
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        builtInZoomControls = false
                        displayZoomControls = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isPageLoading = true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isPageLoading = false
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        private var customView: View? = null
                        private var customCallback: CustomViewCallback? = null

                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            customView = view
                            customCallback = callback
                            customViewContainer = view
                        }

                        override fun onHideCustomView() {
                            customView = null
                            customCallback?.onCustomViewHidden()
                            customViewContainer = null
                        }
                    }

                    loadUrl(previewUrl)
                    webViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading spinner while the Google Drive player prepares
        if (isPageLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF1A94FF), strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Cargando reproductor de Google Drive...", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Conectando con streaming oficial de Drive", color = Color.LightGray, fontSize = 11.sp)
                }
            }
        }

        // Amazon Prime-styled Header Overlay Controls
        AnimatedVisibility(
            visible = showOverlayControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(36.dp).testTag("drive_player_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = movie.title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = Color(0xFF1A73E8)
                            ) {
                                Text(
                                    text = "Google Drive Stream",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                            if (movie.imdbRating.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "IMDb ${movie.imdbRating} ★",
                                    color = Color(0xFFF5C518),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Open in Google Drive App / External Browser Button
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(movie.videoUrl))
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier.height(34.dp).padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF00A8E1), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("App Drive", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Refresh stream button
                    IconButton(
                        onClick = { webViewRef?.reload() },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recargar", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

/**
 * Native Android VideoView Player with custom Prime Video playback controls.
 * Used for direct MP4, MKV, HLS, or WordPress video streams.
 */
@Composable
private fun NativeVideoPlayer(
    movie: Movie,
    viewModel: MovieViewModel,
    onNavigateBack: () -> Unit,
    onSwitchToWebPlayer: () -> Unit
) {
    val context = LocalContext.current
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPos by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val processedUrl = remember(movie.videoUrl) { cleanDirectVideoUrl(movie.videoUrl) }

    // Auto-hide controls after 4 seconds
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    // Coroutine to periodically save playback progress in Room database
    LaunchedEffect(isPlaying, videoViewRef) {
        while (isPlaying && videoViewRef != null) {
            val progress = videoViewRef?.currentPosition?.toLong() ?: 0L
            val total = videoViewRef?.duration?.toLong() ?: 0L
            if (progress > 0) {
                currentPos = progress
                if (total > 0) {
                    duration = total
                    viewModel.updatePlaybackProgress(movie.id, progress, total)
                }
            }
            delay(1000)
        }
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

                        viewModel.viewModelScope.launch {
                            val saved = viewModel.getMoviePlaybackProgress(movie.id).first()
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
                            -1010 -> "Formato de video no soportado directamente"
                            else -> "Este medio requiere streaming web o reproductor externo"
                        }
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

        // Buffering Indicator
        if (isBuffering && errorMessage == null) {
            CircularProgressIndicator(
                color = Color(0xFF1A94FF),
                modifier = Modifier
                    .align(Alignment.Center)
                    .testTag("buffering_indicator")
            )
        }

        // Error message handling with fallback options
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.widthIn(max = 520.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = Color(0xFFFFA000),
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "No se pudo reproducir este medio directamente",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Razón: $errorMessage.",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                        ) {
                            Text("Volver", color = Color.White)
                        }

                        Button(
                            onClick = onSwitchToWebPlayer,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A94FF))
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reproductor Web Integrado")
                        }

                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(movie.videoUrl))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Abrir Externamente")
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
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("video_back_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = movie.title,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (movie.imdbRating.isNotBlank()) {
                                Text(
                                    text = "IMDb ${movie.imdbRating} ★ • ${movie.genre}",
                                    color = Color(0xFFF5C518),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (isGoogleDriveUrl(movie.videoUrl)) {
                        TextButton(onClick = onSwitchToWebPlayer) {
                            Text("Modo Drive Web", color = Color(0xFF00A8E1), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Center Play / Pause Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(36.dp)
                ) {
                    IconButton(
                        onClick = {
                            videoViewRef?.let {
                                val target = (it.currentPosition - 10000).coerceAtLeast(0)
                                it.seekTo(target)
                                currentPos = target.toLong()
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retroceder 10s", tint = Color.White, modifier = Modifier.size(32.dp))
                    }

                    IconButton(
                        onClick = {
                            videoViewRef?.let {
                                if (isPlaying) {
                                    it.pause()
                                    isPlaying = false
                                } else {
                                    it.start()
                                    isPlaying = true
                                }
                            }
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(32.dp))
                            .testTag("play_pause_button")
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausa" else "Reproducir",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            videoViewRef?.let {
                                val target = (it.currentPosition + 10000).coerceAtMost(duration.toInt())
                                it.seekTo(target)
                                currentPos = target.toLong()
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Avanzar 10s", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }

                // Bottom Timeline & Scrubber Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Slider(
                        value = if (duration > 0) (currentPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f,
                        onValueChange = { fraction ->
                            val target = (fraction * duration).toInt()
                            videoViewRef?.seekTo(target)
                            currentPos = target.toLong()
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

/**
 * Checks if a given URL is a Google Drive file link.
 */
fun isGoogleDriveUrl(url: String): Boolean {
    val lower = url.lowercase().trim()
    return lower.contains("drive.google.com") || lower.contains("docs.google.com")
}

/**
 * Extracts Google Drive alphanumeric file identifier.
 */
fun extractGoogleDriveFileId(url: String): String? {
    val regex = Regex("(?:/file/d/|id=|open\\?id=)([a-zA-Z0-9_-]{20,})")
    return regex.find(url.trim())?.groupValues?.get(1)
}

/**
 * Returns Google Drive's official HTML5 streaming embed URL (/preview)
 * which streams smoothly on mobile WebViews with transcoding and player controls.
 */
fun getGoogleDrivePreviewUrl(url: String): String {
    val id = extractGoogleDriveFileId(url)
    return if (id != null) "https://drive.google.com/file/d/$id/preview" else url.trim()
}

/**
 * Normalizes direct WordPress, HTTP, or direct video URLs.
 */
fun cleanDirectVideoUrl(url: String): String {
    val clean = url.trim()
    val driveId = extractGoogleDriveFileId(clean)
    if (driveId != null) {
        return "https://drive.google.com/uc?export=download&id=$driveId"
    }
    return clean
}
