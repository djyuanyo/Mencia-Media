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
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) Color(0xFF00A8E1) else Color(0xFF1E2E4A),
                            modifier = Modifier
                                .clickable { selectedFormatFilter = filterName }
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
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.height(36.dp)
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
                                        .size(36.dp)
                                        .background(Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = "Detalles", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            // 2. RECIENTEMENTE AÑADIDOS (Novedades en tu Biblioteca)
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
                                LandscapeMovieCard(movie, onNavigateToDetail)
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
                                LandscapeMovieCard(movie, onNavigateToDetail)
                            }
                        }
                    }
                }
            }

            // 4. CONTINUE WATCHING (Seguir viendo)
            if (continueWatching.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Text(
                            text = "Continuar viendo",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(continueWatching) { movie ->
                                ContinueWatchingCard(movie, viewModel, onNavigateToDetail, onNavigateToPlayer)
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
                                LandscapeMovieCard(movie, onNavigateToDetail)
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
                                    LandscapeMovieCard(movie, onNavigateToDetail)
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

// 16:9 Aspect Ratio widescreen Cinema Card in Prime Video layout style
@Composable
fun LandscapeMovieCard(
    movie: Movie,
    onClick: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(100.dp)
            .testTag("movie_card_${movie.id}")
            .clickable { onClick(movie.id) },
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2E4A))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Text overlay container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )

            // Badge indicating Ficha / Sin Enlace if videoUrl is blank, or Serie tag
            if (movie.videoUrl.isBlank()) {
                Surface(
                    shape = RoundedCornerShape(topStart = 6.dp, bottomEnd = 4.dp),
                    color = Color(0xFF00A8E1),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = "Ficha",
                        color = Color.Black,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            } else if (movie.category == "Series") {
                Surface(
                    shape = RoundedCornerShape(topStart = 6.dp, bottomEnd = 4.dp),
                    color = Color(0xFF2BAD3B),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = "Serie",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            // IMDb Rating Badge
            if (movie.imdbRating.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(topEnd = 6.dp, bottomStart = 4.dp),
                    color = Color(0xFFF5C518),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = "${movie.imdbRating} ★",
                        color = Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Text(
                text = movie.title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }
    }
}

// Continue watching card layout displaying saved progress index
@Composable
fun ContinueWatchingCard(
    movie: Movie,
    viewModel: MovieViewModel,
    onDetailClick: (Int) -> Unit,
    onPlayClick: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .height(125.dp)
            .clickable { onDetailClick(movie.id) },
        shape = RoundedCornerShape(6.dp),
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

                // Hover play icon button
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { onPlayClick(movie.id) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(18.dp))
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Reanudar", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Bottom title & progress line
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F1E36))
                    .padding(6.dp)
            ) {
                Text(
                    text = movie.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Static thin progress bar (Prime styled orange/blue bar)
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.Gray.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.55f) // Hardcoded progression factor representational index
                            .background(Color(0xFFFF9900)) // Prime Progress Orange
                    )
                }
            }
        }
    }
}
