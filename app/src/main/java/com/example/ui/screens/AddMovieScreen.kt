package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.EpisodeData
import com.example.ui.components.tvFocusable
import com.example.ui.viewmodel.MovieViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMovieScreen(
    viewModel: MovieViewModel,
    onMovieSaved: () -> Unit = {}
) {
    val context = LocalContext.current
    val isAdmin by viewModel.isAdmin.collectAsState()

    // If not admin, show restricted access notice as requested
    if (!isAdmin) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF09111E))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFFF9900),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Acceso Exclusivo de Administrador",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Solo el administrador oficial (juanjocarrillo7@gmail.com) tiene permisos para añadir o modificar contenido en la biblioteca.\n\nEl resto de usuarios registrados pueden navegar y reproducir todo el contenido añadido desde Inicio y Buscar.",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
        return
    }

    // PRIMARY TYPE SELECTOR (Película vs Serie) - Required first choice per user request
    var selectedType by remember { mutableStateOf("Película") } // "Película" or "Serie"

    var searchQuery by remember { mutableStateOf("") }
    val suggestions by viewModel.metadataSuggestions.collectAsState()
    val isSearchingMetadata by viewModel.isSearchingMetadata.collectAsState()

    // Common fields
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var posterUrl by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("Acción") }
    var year by remember { mutableStateOf("2026") }
    var cast by remember { mutableStateOf("") }
    var imdbRating by remember { mutableStateOf("") }
    var imdbId by remember { mutableStateOf("") }
    var tmdbId by remember { mutableStateOf("") }
    var isFeatured by remember { mutableStateOf(false) }
    var isFetchingImdbScore by remember { mutableStateOf(false) }
    var metadataSource by remember { mutableStateOf("TMDB + IMDb + TheTVDB") }
    var appliedSourceNotice by remember { mutableStateOf<String?>(null) }

    // MOVIE specific fields
    var movieVideoUrl by remember { mutableStateOf("") }
    var movieDuration by remember { mutableStateOf("120 min") }

    // SERIES specific fields: Seasons & Chapters with Video Links
    var seriesEpisodes by remember {
        mutableStateOf(
            listOf(
                EpisodeData(
                    seasonNumber = 1,
                    episodeNumber = 1,
                    title = "Capítulo 1",
                    videoUrl = "",
                    runtime = "45 min"
                )
            )
        )
    }

    val genres = listOf("Acción", "Comedia", "Drama", "Sci-Fi", "Fantasía", "Terror", "Documental", "Animación")
    var genreExpanded by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09111E))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = Color(0xFF00A8E1),
                modifier = Modifier
                    .size(40.dp)
                    .padding(bottom = 6.dp)
            )

            Text(
                "Añadir a la Biblioteca",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                "Panel de Administrador • juanjocarrillo7@gmail.com",
                color = Color(0xFF2BAD3B),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // ========================================================
            // 1. STEP 1: SELECT "PELÍCULA" OR "SERIE" (MANDATORY FIRST CHOICE)
            // ========================================================
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .testTag("type_selector_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. Elige el tipo de contenido que vas a añadir:",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Option A: Película
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedType == "Película") Color(0xFF00A8E1) else Color(0xFF1E2E4A),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (selectedType == "Película") 2.dp else 1.dp,
                                color = if (selectedType == "Película") Color.White else Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedType = "Película"
                                    appliedSourceNotice = null
                                }
                                .testTag("select_type_movie")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🎬",
                                    fontSize = 26.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Película",
                                    color = if (selectedType == "Película") Color.Black else Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "1 enlace de vídeo principal",
                                    color = if (selectedType == "Película") Color.Black.copy(alpha = 0.8f) else Color.LightGray,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Option B: Serie
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedType == "Serie") Color(0xFF00A8E1) else Color(0xFF1E2E4A),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (selectedType == "Serie") 2.dp else 1.dp,
                                color = if (selectedType == "Serie") Color.White else Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedType = "Serie"
                                    appliedSourceNotice = null
                                }
                                .testTag("select_type_series")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "📺",
                                    fontSize = 26.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Serie",
                                    color = if (selectedType == "Serie") Color.Black else Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Temporadas y capítulos con enlace",
                                    color = if (selectedType == "Serie") Color.Black.copy(alpha = 0.8f) else Color.LightGray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // ========================================================
            // 2. AUTO METADATA SEARCH ASSISTANT (TMDB / IMDb / TheTVDB)
            // ========================================================
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .testTag("plex_search_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Asistente de Metadatos Oficiales (TMDB / IMDb)",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFF9900).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Autocompletar",
                                color = Color(0xFFFF9900),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Busca el título para rellenar portada, sinopsis y reparto automáticamente:",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Ej: Inception, Stranger Things, Gladiator...", color = Color.Gray, fontSize = 13.sp) },
                            singleLine = true,
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = Color.LightGray)
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00A8E1),
                                unfocusedBorderColor = Color.Gray
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = {
                                if (searchQuery.isNotBlank()) {
                                    viewModel.searchMetadata(searchQuery)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A8E1)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(52.dp)
                        ) {
                            if (isSearchingMetadata) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black)
                            }
                        }
                    }

                    // Suggestions row
                    if (suggestions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(suggestions) { item ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E2E4A),
                                    modifier = Modifier
                                        .width(190.dp)
                                        .clickable {
                                            title = item.title
                                            description = item.description
                                            if (item.posterUrl.isNotEmpty()) posterUrl = item.posterUrl
                                            genre = item.genre
                                            year = item.year
                                            cast = item.cast
                                            imdbRating = item.imdbRating
                                            imdbId = item.imdbId
                                            tmdbId = item.tmdbId
                                            metadataSource = item.source

                                            // If series, load suggested episodes if available
                                            if (selectedType == "Serie") {
                                                if (item.episodes.isNotEmpty()) {
                                                    seriesEpisodes = item.episodes.map { ep ->
                                                        // Preserve existing entered videoUrl if any match
                                                        val existing = seriesEpisodes.find {
                                                            it.seasonNumber == ep.seasonNumber && it.episodeNumber == ep.episodeNumber
                                                        }
                                                        ep.copy(videoUrl = existing?.videoUrl ?: "")
                                                    }
                                                }
                                            } else {
                                                if (item.duration.isNotBlank()) movieDuration = item.duration
                                            }

                                            appliedSourceNotice = "✓ Datos de \"${item.title}\" aplicados con éxito"
                                            Toast.makeText(context, "Metadatos aplicados: ${item.title}", Toast.LENGTH_SHORT).show()
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${item.year} • ${item.genre}", color = Color.LightGray, fontSize = 10.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Pulsa para aplicar", color = Color(0xFF00A8E1), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    appliedSourceNotice?.let { notice ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = notice, color = Color(0xFF2BAD3B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ========================================================
            // 3. TITLE & POSTER FIELDS
            // ========================================================
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(if (selectedType == "Película") "Título de la película" else "Título de la serie", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00A8E1),
                    unfocusedBorderColor = Color.Gray
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .testTag("add_item_title_input")
            )

            OutlinedTextField(
                value = posterUrl,
                onValueChange = { posterUrl = it },
                label = { Text("URL de Portada / Carátula", color = Color.Gray) },
                placeholder = { Text("https://image.tmdb.org/t/p/w500/...", color = Color.DarkGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00A8E1),
                    unfocusedBorderColor = Color.Gray
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            )

            if (posterUrl.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = posterUrl,
                        contentDescription = "Vista previa",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(50.dp, 75.dp).clip(RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Vista previa de la carátula oficial", color = Color.LightGray, fontSize = 12.sp)
                }
            }

            // ========================================================
            // 4. SPECIFIC FIELDS: PELÍCULA VS SERIE
            // ========================================================
            if (selectedType == "Película") {
                // Movie Video URL (Google Drive / WordPress / MP4)
                OutlinedTextField(
                    value = movieVideoUrl,
                    onValueChange = { movieVideoUrl = it },
                    label = { Text("Enlace de Vídeo de la Película (Google Drive o WordPress)", color = Color.Gray) },
                    placeholder = { Text("https://drive.google.com/file/d/... o https://...mp4", color = Color.DarkGray) },
                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF00A8E1)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00A8E1),
                        unfocusedBorderColor = Color.Gray
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .testTag("movie_video_url_input")
                )

                if (com.example.util.GoogleDriveStreamResolver.isGoogleDriveUrl(movieVideoUrl)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF00A8E1).copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "✓ Enlace de Google Drive detectado: se reproducirá directamente con ExoPlayer HD",
                            color = Color(0xFF00A8E1),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = movieDuration,
                        onValueChange = { movieDuration = it },
                        label = { Text("Duración", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00A8E1),
                            unfocusedBorderColor = Color.Gray
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it },
                        label = { Text("Año", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00A8E1),
                            unfocusedBorderColor = Color.Gray
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

            } else {
                // ========================================================
                // SERIE: SECCIÓN DE TEMPORADAS Y CAPÍTULOS CON ENLACE
                // "Si es serie quiero que salga añadir temporadas y añadir capítulos. En cada capítulo pongo enlace."
                // ========================================================
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1E30)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00A8E1).copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .testTag("series_seasons_card")
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
                                    tint = Color(0xFF00A8E1),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Temporadas y Capítulos",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Button to Add New Season
                            Button(
                                onClick = {
                                    val highestSeason = seriesEpisodes.maxOfOrNull { it.seasonNumber } ?: 0
                                    val nextSeason = highestSeason + 1
                                    val newEpisode = EpisodeData(
                                        seasonNumber = nextSeason,
                                        episodeNumber = 1,
                                        title = "Capítulo 1",
                                        videoUrl = "",
                                        runtime = "45 min"
                                    )
                                    seriesEpisodes = seriesEpisodes + newEpisode
                                    Toast.makeText(context, "Temporada $nextSeason añadida", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2BAD3B)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("add_season_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Añadir Temporada", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = "Organiza las temporadas de la serie. En cada capítulo, pega su enlace de vídeo (Google Drive o WordPress):",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        // Distinct seasons grouped
                        val seasonNumbers = seriesEpisodes.map { it.seasonNumber }.distinct().sorted()

                        if (seasonNumbers.isEmpty()) {
                            seriesEpisodes = listOf(
                                EpisodeData(seasonNumber = 1, episodeNumber = 1, title = "Capítulo 1", videoUrl = "")
                            )
                        }

                        seasonNumbers.forEach { seasonNum ->
                            val seasonEpisodes = seriesEpisodes.filter { it.seasonNumber == seasonNum }

                            Spacer(modifier = Modifier.height(14.dp))

                            // SEASON CONTAINER
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF14243B),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A5F)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // Season header
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFF00A8E1).copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "TEMPORADA $seasonNum",
                                                    color = Color(0xFF00A8E1),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "(${seasonEpisodes.size} capítulos)",
                                                color = Color.LightGray,
                                                fontSize = 11.sp
                                            )
                                        }

                                        // Button to add chapter to this season
                                        TextButton(
                                            onClick = {
                                                val nextEpNum = (seasonEpisodes.maxOfOrNull { it.episodeNumber } ?: 0) + 1
                                                val newEp = EpisodeData(
                                                    seasonNumber = seasonNum,
                                                    episodeNumber = nextEpNum,
                                                    title = "Capítulo $nextEpNum",
                                                    videoUrl = "",
                                                    runtime = "45 min"
                                                )
                                                seriesEpisodes = seriesEpisodes + newEp
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF00A8E1), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("+ Añadir Capítulo", color = Color(0xFF00A8E1), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // List of chapters inside this season
                                    seasonEpisodes.forEachIndexed { idxInSeason, ep ->
                                        val overallIndex = seriesEpisodes.indexOf(ep)

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF0F1E36),
                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF223A5E)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = "Capítulo ${ep.episodeNumber}",
                                                        color = Color(0xFF00A8E1),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )

                                                    // Delete chapter button
                                                    if (seriesEpisodes.size > 1) {
                                                        IconButton(
                                                            onClick = {
                                                                seriesEpisodes = seriesEpisodes.toMutableList().also {
                                                                    it.removeAt(overallIndex)
                                                                }
                                                            },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Delete,
                                                                contentDescription = "Eliminar capítulo",
                                                                tint = Color(0xFFFF6B6B),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                // Title of episode
                                                OutlinedTextField(
                                                    value = ep.title,
                                                    onValueChange = { newTitle ->
                                                        seriesEpisodes = seriesEpisodes.toMutableList().also {
                                                            it[overallIndex] = ep.copy(title = newTitle)
                                                        }
                                                    },
                                                    label = { Text("Nombre del capítulo", color = Color.Gray, fontSize = 11.sp) },
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedTextColor = Color.White,
                                                        unfocusedTextColor = Color.White,
                                                        focusedBorderColor = Color(0xFF00A8E1),
                                                        unfocusedBorderColor = Color.Gray
                                                    ),
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))

                                                // VIDEO URL FOR THIS CHAPTER
                                                OutlinedTextField(
                                                    value = ep.videoUrl,
                                                    onValueChange = { newUrl ->
                                                        seriesEpisodes = seriesEpisodes.toMutableList().also {
                                                            it[overallIndex] = ep.copy(videoUrl = newUrl.trim())
                                                        }
                                                    },
                                                    label = { Text("Enlace de vídeo del capítulo (Google Drive / WordPress)", color = Color(0xFFFF9900), fontSize = 11.sp) },
                                                    placeholder = { Text("https://drive.google.com/... o enlace directo", color = Color.DarkGray, fontSize = 11.sp) },
                                                    leadingIcon = {
                                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFFFF9900), modifier = Modifier.size(18.dp))
                                                    },
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedTextColor = Color.White,
                                                        unfocusedTextColor = Color.White,
                                                        focusedBorderColor = Color(0xFFFF9900),
                                                        unfocusedBorderColor = Color.Gray
                                                    ),
                                                    singleLine = true,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .testTag("episode_video_url_${seasonNum}_${ep.episodeNumber}")
                                                )

                                                if (com.example.util.GoogleDriveStreamResolver.isGoogleDriveUrl(ep.videoUrl)) {
                                                    Text(
                                                        text = "✓ Enlace de Google Drive asignado",
                                                        color = Color(0xFF2BAD3B),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(top = 2.dp, start = 4.dp)
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

                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it },
                    label = { Text("Año de la serie", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00A8E1),
                        unfocusedBorderColor = Color.Gray
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                )
            }

            // ========================================================
            // 5. GENRE & CAST & SINOPSIS (COMMON)
            // ========================================================
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                OutlinedTextField(
                    value = genre,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Género", color = Color.Gray) },
                    trailingIcon = {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.clickable { genreExpanded = !genreExpanded }
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00A8E1),
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth().clickable { genreExpanded = !genreExpanded }
                )
                DropdownMenu(
                    expanded = genreExpanded,
                    onDismissRequest = { genreExpanded = false },
                    modifier = Modifier.background(Color(0xFF1E2E4A)).fillMaxWidth(0.9f)
                ) {
                    genres.forEach { gen ->
                        DropdownMenuItem(
                            text = { Text(gen, color = Color.White) },
                            onClick = {
                                genre = gen
                                genreExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = cast,
                onValueChange = { cast = it },
                label = { Text("Reparto / Actores", color = Color.Gray) },
                placeholder = { Text("Ej: Pedro Pascal, Bella Ramsey...", color = Color.DarkGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00A8E1),
                    unfocusedBorderColor = Color.Gray
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Sinopsis / Resumen", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00A8E1),
                    unfocusedBorderColor = Color.Gray
                ),
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
            )

            // Destacar toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .clickable { isFeatured = !isFeatured }
            ) {
                Checkbox(
                    checked = isFeatured,
                    onCheckedChange = { isFeatured = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF00A8E1),
                        checkmarkColor = Color.Black
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Destacar en carrusel principal de Inicio",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // ========================================================
            // 6. SAVE BUTTON
            // ========================================================
            Button(
                onClick = {
                    if (title.trim().isEmpty()) {
                        Toast.makeText(context, "Por favor escribe al menos el título", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (selectedType == "Película") {
                        viewModel.addMovie(
                            title = title.trim(),
                            description = description.trim().ifEmpty { "Sin sinopsis disponible." },
                            videoUrl = movieVideoUrl.trim(),
                            posterUrl = posterUrl.trim(),
                            category = "Películas",
                            genre = genre,
                            year = year.ifEmpty { "2026" },
                            duration = movieDuration.ifEmpty { "120 min" },
                            isFeatured = isFeatured,
                            cast = cast.trim(),
                            imdbRating = imdbRating.trim(),
                            imdbId = imdbId.trim(),
                            tmdbId = tmdbId.trim(),
                            episodesJson = "",
                            metadataSource = metadataSource.ifEmpty { "TMDB + IMDb" }
                        )
                        Toast.makeText(context, "¡Película \"${title.trim()}\" guardada con éxito!", Toast.LENGTH_SHORT).show()
                    } else {
                        // Series: Convert seasons and chapters
                        val seasonsCount = seriesEpisodes.map { it.seasonNumber }.distinct().size
                        val totalEps = seriesEpisodes.size
                        val formattedDuration = "$totalEps caps ($seasonsCount temp)"
                        val jsonEpisodes = EpisodeData.listToJson(seriesEpisodes)

                        // If series has chapters with video, pick first chapter's video as default fallback videoUrl
                        val firstVideo = seriesEpisodes.firstOrNull { it.videoUrl.isNotBlank() }?.videoUrl ?: ""

                        viewModel.addMovie(
                            title = title.trim(),
                            description = description.trim().ifEmpty { "Sin sinopsis disponible." },
                            videoUrl = firstVideo,
                            posterUrl = posterUrl.trim(),
                            category = "Series",
                            genre = genre,
                            year = year.ifEmpty { "2026" },
                            duration = formattedDuration,
                            isFeatured = isFeatured,
                            cast = cast.trim(),
                            imdbRating = imdbRating.trim(),
                            imdbId = imdbId.trim(),
                            tmdbId = tmdbId.trim(),
                            episodesJson = jsonEpisodes,
                            metadataSource = metadataSource.ifEmpty { "TMDB + TheTVDB" }
                        )
                        Toast.makeText(context, "¡Serie \"${title.trim()}\" guardada con $totalEps capítulos en $seasonsCount temporadas!", Toast.LENGTH_SHORT).show()
                    }

                    onMovieSaved()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A8E1)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .tvFocusable(shape = RoundedCornerShape(8.dp), focusedScale = 1.04f)
                    .testTag("submit_movie_button")
            ) {
                Text(
                    text = if (selectedType == "Película") "Guardar Película en la Biblioteca" else "Guardar Serie en la Biblioteca",
                    color = Color.Black,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
