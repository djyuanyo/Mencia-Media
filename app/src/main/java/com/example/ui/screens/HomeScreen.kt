package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.data.model.Movie
import com.example.ui.components.tvFocusable
import com.example.ui.viewmodel.MovieViewModel

@Composable
fun HomeScreen(
    viewModel: MovieViewModel,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToPlayer: (Int) -> Unit
) {
    val context = LocalContext.current
    val activeProfile by viewModel.currentProfile.collectAsState()
    val allMovies by viewModel.allMovies.collectAsState()
    val watchlist by viewModel.watchlist.collectAsState()
    val continueWatching by viewModel.continueWatching.collectAsState()
    val allPlaybackProgresses by viewModel.allPlaybackProgresses.collectAsState(initial = emptyMap())

    var selectedFormatFilter by remember { mutableStateOf("Todo") }
    val formatFilters = listOf("Todo", "Películas", "Series")

    // Filter contents dynamically if user is a children profile
    val baseMovies = remember(allMovies, activeProfile) {
        if (activeProfile?.isKid == true) {
            allMovies.filter { it.genre.lowercase() in listOf("comedia", "fantasía") }
        } else {
            allMovies
        }
    }

    // Filter by format tab (Todo / Películas / Series)
    val displayedMovies = remember(baseMovies, selectedFormatFilter) {
        when (selectedFormatFilter) {
            "Películas" -> baseMovies.filter { it.category.equals("Películas", ignoreCase = true) }
            "Series" -> baseMovies.filter { it.category.equals("Series", ignoreCase = true) }
            else -> baseMovies
        }
    }

    // Hero image selection
    val featuredMovie = remember(displayedMovies) {
        displayedMovies.firstOrNull { it.isFeatured } ?: displayedMovies.firstOrNull()
    }

    // Recently added items sorted by newest addedAt
    val recentlyAdded = remember(displayedMovies) {
        displayedMovies.sortedByDescending { it.addedAt }
    }

    // Series items
    val seriesList = remember(displayedMovies) {
        displayedMovies.filter { it.category.equals("Series", ignoreCase = true) }
    }

    val genres = remember(displayedMovies) {
        displayedMovies.map { it.genre }.distinct()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09111E)) // Deep dark Slate blue
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_screen_column")
        ) {
            // FORMAT FILTER PILLS (Todo / Películas / Series)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    formatFilters.forEach { filterName ->
                        val isSelected = selectedFormatFilter == filterName
                        val chipInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) Color(0xFF00A8E1) else Color(0xFF1E2E4A),
                            modifier = Modifier
                                .tvFocusable(
                                    shape = RoundedCornerShape(16.dp),
                                    focusedBorderColor = Color.White,
                                    focusedScale = 1.08f,
                                    interactionSource = chipInteractionSource
                                )
                                .clickable(
                                    interactionSource = chipInteractionSource,
                                    indication = null
                                ) { selectedFormatFilter = filterName }
                                .testTag("home_filter_$filterName")
                        ) {
                            Text(
                                text = filterName,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // 1. HERO HEADER (Prime Video Widescreen Featured Banner)
            featuredMovie?.let { movie ->
                item {
                    val hasVideo = movie.videoUrl.isNotBlank()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clickable { onNavigateToDetail(movie.id) }
                    ) {
                        AsyncImage(
                            model = movie.posterUrl,
                            contentDescription = movie.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Prime Blue-Deep Blue shading gradients
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF09111E).copy(alpha = 0.2f),
                                            Color(0xFF09111E).copy(alpha = 0.6f),
                                            Color(0xFF09111E)
                                        )
                                    )
                                )
                        )

                        // Movie overlay details text
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(20.dp)
                                .fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (movie.category == "Series") "SERIE DESTACADA" else "DESTAQUE HOY",
                                    color = Color(0xFF00A8E1),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                if (movie.imdbRating.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFF5C518)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "IMDb",
                                                color = Color.Black,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "${movie.imdbRating} ★",
                                                color = Color.Black,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }

                                if (!hasVideo) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF334155).copy(alpha = 0.8f)
                                    ) {
                                        Text(
                                            text = "FICHA / SIN ENLACE",
                                            color = Color.LightGray,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = movie.title,
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                            )

                            Text(
                                text = movie.description,
                                color = Color.LightGray,
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        if (hasVideo) {
                                            onNavigateToPlayer(movie.id)
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Este contenido no tiene enlace de vídeo para reproducir.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            onNavigateToDetail(movie.id)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (hasVideo) Color(0xFF1A94FF) else Color(0xFF27354A)
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .height(40.dp)
                                        .tvFocusable(
                                            shape = RoundedCornerShape(6.dp),
                                            focusedBorderColor = Color.White,
                                            focusedScale = 1.06f
                                        )
                                        .testTag("hero_play_button")
                                ) {
                                    Icon(
                                        if (hasVideo) Icons.Default.PlayArrow else Icons.Default.Info,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (hasVideo) "Ver ahora" else "Ver ficha",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                IconButton(
                                    onClick = { onNavigateToDetail(movie.id) },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .tvFocusable(
                                            shape = RoundedCornerShape(6.dp),
                                            focusedBorderColor = Color(0xFF00A8E1),
                                            focusedScale = 1.08f
                                        )
                                        .background(Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                                        .testTag("hero_details_button")
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = "Detalles", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            // 2. CONTINUAR VIENDO (Seguir viendo en el minuto y segundo exacto)
            if (continueWatching.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Continuar viendo",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFFF9900).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "En progreso",
                                    color = Color(0xFFFF9900),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(continueWatching) { movie ->
                                ContinueWatchingCard(movie, viewModel, onNavigateToDetail, onNavigateToPlayer)
                            }
                        }
                    }
                }
            }

            // 3. RECIENTEMENTE AÑADIDOS (Novedades en tu Biblioteca)
            // Displays all items sorted by addedAt descending so newly saved titles appear first!
            if (recentlyAdded.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Añadidos recientemente",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF00A8E1).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Novedades",
                                    color = Color(0xFF00A8E1),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(recentlyAdded) { movie ->
                                LandscapeMovieCard(
                                    movie = movie,
                                    isWatched = allPlaybackProgresses[movie.id]?.isWatched() == true,
                                    onClick = onNavigateToDetail
                                )
                            }
                        }
                    }
                }
            }

            // 3. SERIES DE TELEVISIÓN (Dedicated row for series when in Todo or Series filter)
            if (seriesList.isNotEmpty() && selectedFormatFilter != "Películas") {
                item {
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Series de televisión",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF2BAD3B).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "${seriesList.size} series",
                                    color = Color(0xFF2BAD3B),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(seriesList) { movie ->
                                LandscapeMovieCard(
                                    movie = movie,
                                    isWatched = allPlaybackProgresses[movie.id]?.isWatched() == true,
                                    onClick = onNavigateToDetail
                                )
                            }
                        }
                    }
                }
            }

            // 5. WATCHLIST / LISTA DE SEGUIMIENTO (Mi Lista)
            if (watchlist.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Text(
                            text = "Mi lista de seguimiento",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(watchlist) { movie ->
                                LandscapeMovieCard(
                                    movie = movie,
                                    isWatched = allPlaybackProgresses[movie.id]?.isWatched() == true,
                                    onClick = onNavigateToDetail
                                )
                            }
                        }
                    }
                }
            }

            // 6. CATEGORIES BY GENRE
            genres.forEach { genre ->
                val genreMovies = displayedMovies.filter { it.genre == genre }
                if (genreMovies.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(vertical = 12.dp)) {
                            Text(
                                text = when (selectedFormatFilter) {
                                    "Series" -> "Series de $genre"
                                    "Películas" -> "Películas de $genre"
                                    else -> "Títulos de $genre"
                                },
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(genreMovies) { movie ->
                                    LandscapeMovieCard(
                                        movie = movie,
                                        isWatched = allPlaybackProgresses[movie.id]?.isWatched() == true,
                                        onClick = onNavigateToDetail
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Margin bottom
            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

// Vertical Aspect Ratio (2:3) Movie & Series Poster Card
@Composable
fun LandscapeMovieCard(
    movie: Movie,
    onClick: (Int) -> Unit,
    isWatched: Boolean = false
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Card(
        modifier = Modifier
            .width(135.dp)
            .height(202.dp)
            .testTag("movie_card_${movie.id}")
            .tvFocusable(
                shape = RoundedCornerShape(10.dp),
                focusedBorderColor = Color(0xFF00A8E1),
                focusedScale = 1.08f,
                interactionSource = interactionSource
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick(movie.id) },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2E4A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Bottom gradient overlay for legible titles
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            // Top Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                if (isWatched) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF2BAD3B)
                    ) {
                        Text(
                            text = "✓ VISTO",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                } else if (movie.category.equals("Series", ignoreCase = true)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF2BAD3B)
                    ) {
                        Text(
                            text = "SERIE",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                } else if (movie.videoUrl.isBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF00A8E1)
                    ) {
                        Text(
                            text = "FICHA",
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // IMDb Rating Badge
                if (movie.imdbRating.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFF5C518)
                    ) {
                        Text(
                            text = "★ ${movie.imdbRating}",
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Bottom Info: Title & Year
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Text(
                    text = movie.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = movie.year,
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = movie.genre,
                        color = Color(0xFF00A8E1),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// Continue watching card layout displaying real progress timestamp and direct resume
@Composable
fun ContinueWatchingCard(
    movie: Movie,
    viewModel: MovieViewModel,
    onDetailClick: (Int) -> Unit,
    onPlayClick: (Int) -> Unit
) {
    val cardInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val progressDetails by viewModel.getMoviePlaybackProgressDetails(movie.id).collectAsState(initial = null)
    val hasVideo = movie.videoUrl.isNotBlank()

    Card(
        modifier = Modifier
            .width(200.dp)
            .height(140.dp)
            .tvFocusable(
                shape = RoundedCornerShape(8.dp),
                focusedBorderColor = Color(0xFF00A8E1),
                focusedScale = 1.06f,
                interactionSource = cardInteractionSource
            )
            .clickable(
                interactionSource = cardInteractionSource,
                indication = null
            ) {
                if (hasVideo) onPlayClick(movie.id) else onDetailClick(movie.id)
            },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2E4A))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = movie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Timestamp Badge Overlay
                val timeLabel = progressDetails?.formatProgressTime() ?: "00:00"
                Surface(
                    shape = RoundedCornerShape(bottomEnd = 6.dp),
                    color = Color.Black.copy(alpha = 0.8f),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFFFF9900), RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (movie.category == "Series" && (progressDetails?.episodeNumber ?: 0) > 0) {
                                "T${progressDetails?.seasonNumber ?: 1}:E${progressDetails?.episodeNumber ?: 1} • $timeLabel"
                            } else {
                                "Minuto $timeLabel"
                            },
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Play icon button
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { onPlayClick(movie.id) },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(19.dp))
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Reanudar", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }

            // Bottom title & progress line
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F1E36))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = movie.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val timeRemaining = if (progressDetails != null && progressDetails!!.durationMs > progressDetails!!.progressMs) {
                    val remainingMs = progressDetails!!.durationMs - progressDetails!!.progressMs
                    val remMin = (remainingMs / 1000) / 60
                    if (remMin > 0) "Quedan $remMin min • En ${progressDetails?.formatProgressTime()}" else "En ${progressDetails?.formatProgressTime()}"
                } else {
                    "Seguir viendo en ${progressDetails?.formatProgressTime() ?: "0:00"}"
                }

                Text(
                    text = timeRemaining,
                    color = Color(0xFF00A8E1),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Dynamic progress bar reflecting real progress fraction
                val frac = progressDetails?.getProgressFraction() ?: 0.1f
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.Gray.copy(alpha = 0.35f), RoundedCornerShape(2.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(frac.coerceIn(0.05f, 1f))
                            .background(Color(0xFFFF9900), RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}
