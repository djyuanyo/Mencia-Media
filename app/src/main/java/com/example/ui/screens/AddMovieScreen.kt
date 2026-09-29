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
import com.example.ui.viewmodel.MovieViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMovieScreen(
    viewModel: MovieViewModel,
    onMovieSaved: () -> Unit = {}
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val suggestions by viewModel.metadataSuggestions.collectAsState()
    val isSearchingMetadata by viewModel.isSearchingMetadata.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var videoUrl by remember { mutableStateOf("") }
    var posterUrl by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Películas") }
    var genre by remember { mutableStateOf("Acción") }
    var year by remember { mutableStateOf("2026") }
    var duration by remember { mutableStateOf("120 min") }
    var cast by remember { mutableStateOf("") }
    var imdbRating by remember { mutableStateOf("") }
    var imdbId by remember { mutableStateOf("") }
    var tmdbId by remember { mutableStateOf("") }
    var episodesList by remember { mutableStateOf<List<EpisodeData>>(emptyList()) }
    var metadataSource by remember { mutableStateOf("TMDB + IMDb + TheTVDB") }
    var isFeatured by remember { mutableStateOf(false) }

    var appliedSourceNotice by remember { mutableStateOf<String?>(null) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var customKeyInput by remember { mutableStateOf(viewModel.getCustomTmdbKey()) }

    val categories = listOf("Películas", "Series", "Documentales")
    val genres = listOf("Acción", "Comedia", "Drama", "Sci-Fi", "Fantasía", "Terror", "Documental", "Animación")

    var categoryExpanded by remember { mutableStateOf(false) }
    var genreExpanded by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // API Key Dialog
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = {
                Text(
                    "Configuración de Fuentes de Metadatos",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column {
                    Text(
                        "PrimePlex busca metadatos simultáneamente en las 3 bibliotecas oficiales:\n" +
                                "• TMDB (The Movie Database): Resúmenes y reparto en español.\n" +
                                "• IMDb: Puntuaciones oficiales y valoraciones.\n" +
                                "• TheTVDB: Orden de episodios y temporadas para series.\n\n" +
                                "Si tienes tu propia clave de TMDB v3 puedes ingresarla aquí (o dejarla vacía para usar la clave estándar integrada):",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customKeyInput,
                        onValueChange = { customKeyInput = it },
                        label = { Text("Clave API TMDB v3 (Opcional)", color = Color.Gray) },
                        placeholder = { Text("Clave API de 32 caracteres", color = Color.DarkGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00A8E1),
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setCustomTmdbKey(customKeyInput)
                        showApiKeyDialog = false
                        Toast.makeText(context, "Configuración guardada", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A8E1))
                ) {
                    Text("Guardar", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cerrar", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF0F1E36)
        )
    }

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
                    .size(44.dp)
                    .padding(bottom = 8.dp)
            )

            Text(
                "Añadir a Biblioteca",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                "Reconocimiento de TMDB, IMDb y TheTVDB estilo Plex",
                color = Color(0xFF00A8E1),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 1. PLEX-STYLE AUTO METADATA SEARCH CARD (TMDB + IMDb + TheTVDB)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "plex",
                                color = Color(0xFFFF9900),
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Agente de Metadatos Multi-Fuente",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = { showApiKeyDialog = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Ajustes de fuentes",
                                tint = Color(0xFF00A8E1),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3 Source Indicator Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // TMDB Badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF01B4E4).copy(alpha = 0.2f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("TMDB", color = Color(0xFF01B4E4), fontSize = 10.sp, fontWeight = FontWeight.Black)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Resúmenes", color = Color.White, fontSize = 9.sp)
                            }
                        }

                        // IMDb Badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFF5C518).copy(alpha = 0.2f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("IMDb", color = Color(0xFFF5C518), fontSize = 10.sp, fontWeight = FontWeight.Black)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Puntuación", color = Color.White, fontSize = 9.sp)
                            }
                        }

                        // TheTVDB Badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF2BAD3B).copy(alpha = 0.2f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("TheTVDB", color = Color(0xFF2BAD3B), fontSize = 10.sp, fontWeight = FontWeight.Black)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Episodios", color = Color.White, fontSize = 9.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Busca por título para obtener sinopsis y reparto de TMDB, valoraciones de IMDb y orden de episodios de TheTVDB:",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Ej: Inception, Breaking Bad, Avatar...", color = Color.Gray, fontSize = 13.sp) },
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
                                unfocusedBorderColor = Color.Gray,
                                cursorColor = Color(0xFF00A8E1)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("plex_search_input")
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = {
                                if (searchQuery.isNotBlank()) {
                                    viewModel.searchMetadata(searchQuery)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00A8E1),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(56.dp)
                                .testTag("plex_search_button")
                        ) {
                            if (isSearchingMetadata) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Search, contentDescription = "Buscar")
                            }
                        }
                    }

                    // Metadata results carousel
                    AnimatedVisibility(visible = suggestions.isNotEmpty()) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Coincidencias encontradas (${suggestions.size}):",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = { viewModel.clearMetadataSuggestions() }) {
                                    Text("Ocultar", color = Color.Gray, fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(suggestions) { item ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2E4A)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .width(240.dp)
                                            .border(1.dp, Color(0xFF00A8E1).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .clickable {
                                                title = item.title
                                                description = item.description
                                                if (item.posterUrl.isNotEmpty()) posterUrl = item.posterUrl
                                                category = item.category
                                                genre = item.genre
                                                year = item.year
                                                duration = item.duration
                                                cast = item.cast
                                                imdbRating = item.imdbRating
                                                imdbId = item.imdbId
                                                tmdbId = item.tmdbId
                                                episodesList = item.episodes
                                                metadataSource = item.source
                                                appliedSourceNotice = "✓ Datos aplicados: ${item.title} (${item.year}) • ${item.source}"
                                                Toast.makeText(context, "Metadatos aplicados: ${item.title}", Toast.LENGTH_SHORT).show()
                                            }
                                            .testTag("suggestion_card_${item.title}")
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(verticalAlignment = Alignment.Top) {
                                                if (item.posterUrl.isNotEmpty()) {
                                                    AsyncImage(
                                                        model = item.posterUrl,
                                                        contentDescription = item.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .width(60.dp)
                                                            .height(85.dp)
                                                            .clip(RoundedCornerShape(4.dp))
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.title,
                                                        color = Color.White,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "${item.year} • ${item.category}",
                                                        color = Color(0xFF00A8E1),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )

                                                    // IMDb rating score pill
                                                    if (item.imdbRating.isNotBlank()) {
                                                        Surface(
                                                            shape = RoundedCornerShape(3.dp),
                                                            color = Color(0xFFF5C518),
                                                            modifier = Modifier.padding(top = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = "IMDb ${item.imdbRating} ★",
                                                                color = Color.Black,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Black,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }

                                                    Text(
                                                        text = item.genre,
                                                        color = Color.LightGray,
                                                        fontSize = 10.sp,
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            // Source badge
                                            Surface(
                                                shape = RoundedCornerShape(3.dp),
                                                color = Color(0xFF0F1E36)
                                            ) {
                                                Text(
                                                    text = item.source,
                                                    color = Color(0xFF00A8E1),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Text(
                                                text = item.description,
                                                color = Color.Gray,
                                                fontSize = 10.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            if (item.cast.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = "Reparto: ${item.cast}",
                                                    color = Color(0xFFFF9900),
                                                    fontSize = 9.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            // TheTVDB Episode count indicator
                                            if (item.episodes.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = "TheTVDB: ${item.episodes.size} episodios ordenados",
                                                    color = Color(0xFF2BAD3B),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Button(
                                                onClick = {
                                                    title = item.title
                                                    description = item.description
                                                    if (item.posterUrl.isNotEmpty()) posterUrl = item.posterUrl
                                                    category = item.category
                                                    genre = item.genre
                                                    year = item.year
                                                    duration = item.duration
                                                    cast = item.cast
                                                    imdbRating = item.imdbRating
                                                    imdbId = item.imdbId
                                                    tmdbId = item.tmdbId
                                                    episodesList = item.episodes
                                                    metadataSource = item.source
                                                    appliedSourceNotice = "✓ Datos aplicados: ${item.title} (${item.year}) • ${item.source}"
                                                    Toast.makeText(context, "Metadatos aplicados: ${item.title}", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF1A94FF),
                                                    contentColor = Color.White
                                                ),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(30.dp)
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Usar estos datos", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Notice when applied
                    appliedSourceNotice?.let { notice ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = notice,
                            color = Color(0xFF2BAD3B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 2. VIDEO STREAM LINK (OPCIONAL)
            OutlinedTextField(
                value = videoUrl,
                onValueChange = { newUrl ->
                    videoUrl = newUrl
                    if (title.isBlank() && newUrl.isNotBlank()) {
                        val extracted = viewModel.extractCleanTitle(newUrl)
                        if (extracted.isNotBlank() && !extracted.startsWith("http")) {
                            searchQuery = extracted
                            title = extracted
                        }
                    }
                },
                label = { Text("Enlace de Vídeo / Streaming (Opcional)", color = Color.Gray) },
                placeholder = { Text("https://... (Opcional: puedes guardar sin enlace)", color = Color.DarkGray) },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray) },
                trailingIcon = {
                    if (videoUrl.isNotEmpty()) {
                        TextButton(onClick = {
                            val extracted = viewModel.extractCleanTitle(videoUrl)
                            if (extracted.isNotBlank()) {
                                searchQuery = extracted
                                viewModel.searchMetadata(extracted)
                            }
                        }) {
                            Text("Detectar", color = Color(0xFF00A8E1), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00A8E1),
                    unfocusedBorderColor = Color.Gray,
                    cursorColor = Color(0xFF00A8E1)
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .testTag("add_item_video_url_input")
            )

            Text(
                "Opcional. Acepta enlaces directos de Google Drive o WordPress. Si no tienes enlace, guárdalo igualmente: aparecerá en tu biblioteca y página de inicio con todos sus datos.",
                color = Color.LightGray,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, start = 4.dp)
            )

            // 3. TITLE OF MEDIA
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título de la obra (TMDB / IMDb)", color = Color.Gray) },
                trailingIcon = {
                    if (title.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = title
                            viewModel.searchMetadata(title)
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar metadatos", tint = Color(0xFF00A8E1))
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00A8E1),
                    unfocusedBorderColor = Color.Gray,
                    cursorColor = Color(0xFF00A8E1)
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("add_item_title_input")
            )

            // 4. POSTER URL
            OutlinedTextField(
                value = posterUrl,
                onValueChange = { posterUrl = it },
                label = { Text("URL de Portada / Póster (Detectado de TMDB / TheTVDB)", color = Color.Gray) },
                placeholder = { Text("https://image.tmdb.org/t/p/w500/...", color = Color.DarkGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00A8E1),
                    unfocusedBorderColor = Color.Gray,
                    cursorColor = Color(0xFF00A8E1)
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // Preview poster thumbnail if available
            if (posterUrl.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = posterUrl,
                        contentDescription = "Vista previa portada",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(60.dp, 85.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Vista previa de la carátula oficial", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Obtenida desde ${metadataSource.ifEmpty { "TMDB" }}", color = Color(0xFF00A8E1), fontSize = 11.sp)
                    }
                }
            }

            // 5. IMDB RATING & IMDB ID ROW
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = imdbRating,
                    onValueChange = { imdbRating = it },
                    label = { Text("Puntuación IMDb (⭐)", color = Color.Gray) },
                    placeholder = { Text("Ej: 8.8", color = Color.DarkGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFF5C518),
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = Color(0xFFF5C518)
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = imdbId,
                    onValueChange = { imdbId = it },
                    label = { Text("ID de IMDb", color = Color.Gray) },
                    placeholder = { Text("Ej: tt1375666", color = Color.DarkGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFF5C518),
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = Color(0xFFF5C518)
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // 6. ACTORS / REPARTO (TMDB)
            OutlinedTextField(
                value = cast,
                onValueChange = { cast = it },
                label = { Text("Actores y Reparto (The Movie Database TMDB)", color = Color.Gray) },
                placeholder = { Text("Ej: Leonardo DiCaprio, Joseph Gordon-Levitt, Elliot Page...", color = Color.DarkGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00A8E1),
                    unfocusedBorderColor = Color.Gray,
                    cursorColor = Color(0xFF00A8E1)
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("add_item_cast_input")
            )

            // 7. CATEGORY SELECTOR
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría (Películas / Series)", color = Color.Gray) },
                    trailingIcon = {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.clickable { categoryExpanded = !categoryExpanded }
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00A8E1),
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth().clickable { categoryExpanded = !categoryExpanded }
                )
                DropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false },
                    modifier = Modifier.background(Color(0xFF1E2E4A)).fillMaxWidth(0.9f)
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat, color = Color.White) },
                            onClick = {
                                category = cat
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            // 8. GENRE SELECTOR
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                OutlinedTextField(
                    value = genre,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Género Cinematográfico", color = Color.Gray) },
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

            // 9. YEAR AND DURATION
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it },
                    label = { Text("Año", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00A8E1),
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = Color(0xFF00A8E1)
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("Duración / Episodios", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00A8E1),
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = Color(0xFF00A8E1)
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // 10. DESCRIPTION / SINOPSIS (TMDB)
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Sinopsis de la obra (The Movie Database TMDB)", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00A8E1),
                    unfocusedBorderColor = Color.Gray,
                    cursorColor = Color(0xFF00A8E1)
                ),
                minLines = 3,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // 11. THETVDB ORDERED EPISODES PREVIEW (For Series)
            if (category == "Series" && episodesList.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2214)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = Color(0xFF2BAD3B), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Guía de Episodios (Orden TheTVDB): ${episodesList.size} eps",
                                color = Color(0xFF2BAD3B),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        episodesList.take(4).forEach { ep ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "T${ep.seasonNumber}E${ep.episodeNumber}: ${ep.title}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(ep.runtime, color = Color.LightGray, fontSize = 11.sp)
                            }
                        }
                        if (episodesList.size > 4) {
                            Text(
                                "...y ${episodesList.size - 4} episodios más guardados en el orden de TheTVDB",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // 12. FEATURED TOGGLE
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
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
                Column {
                    Text(
                        "Destacar portada en carrusel principal",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Aparecerá en el banner panorámico superior de Inicio.",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }

            // SUBMIT BUTTON
            Button(
                onClick = {
                    if (title.trim().isEmpty()) {
                        Toast.makeText(context, "Por favor escribe al menos el título de la película o serie", Toast.LENGTH_SHORT).show()
                    } else {
                        val episodesJson = if (episodesList.isNotEmpty()) {
                            EpisodeData.listToJson(episodesList)
                        } else ""

                        viewModel.addMovie(
                            title = title.trim(),
                            description = description.trim().ifEmpty { "Sin sinopsis disponible." },
                            videoUrl = videoUrl.trim(),
                            posterUrl = posterUrl.trim(),
                            category = category,
                            genre = genre,
                            year = year.ifEmpty { "2026" },
                            duration = duration.ifEmpty { if (category == "Series") "${episodesList.size.coerceAtLeast(1)} eps" else "120 min" },
                            isFeatured = isFeatured,
                            cast = cast.trim(),
                            imdbRating = imdbRating.trim(),
                            imdbId = imdbId.trim(),
                            tmdbId = tmdbId.trim(),
                            episodesJson = episodesJson,
                            metadataSource = metadataSource.ifEmpty { "TMDB + IMDb + TheTVDB" }
                        )
                        Toast.makeText(context, "¡\"${title.trim()}\" guardado con éxito en tu biblioteca!", Toast.LENGTH_SHORT).show()

                        // Reset forms
                        title = ""
                        description = ""
                        videoUrl = ""
                        posterUrl = ""
                        cast = ""
                        imdbRating = ""
                        imdbId = ""
                        tmdbId = ""
                        episodesList = emptyList()
                        isFeatured = false
                        appliedSourceNotice = null

                        onMovieSaved()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00A8E1), // Prime Blue
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_movie_button")
            ) {
                Text("Guardar en mi Biblioteca", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
