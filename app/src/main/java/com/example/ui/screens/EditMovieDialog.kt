package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.EpisodeData
import com.example.data.model.Movie
import com.example.ui.viewmodel.MovieViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMovieDialog(
    movie: Movie,
    viewModel: MovieViewModel,
    onDismiss: () -> Unit,
    onUpdated: (Movie) -> Unit
) {
    val context = LocalContext.current
    val isSeries = movie.category.equals("Series", ignoreCase = true)

    var title by remember { mutableStateOf(movie.title) }
    var description by remember { mutableStateOf(movie.description) }
    var posterUrl by remember { mutableStateOf(movie.posterUrl) }
    var genre by remember { mutableStateOf(movie.genre) }
    var year by remember { mutableStateOf(movie.year) }
    var duration by remember { mutableStateOf(movie.duration) }
    var cast by remember { mutableStateOf(movie.cast) }

    // Video URL for movie
    var movieVideoUrl by remember { mutableStateOf(movie.videoUrl) }

    // Chapters for series
    var seriesEpisodes by remember {
        mutableStateOf(
            if (isSeries) {
                val parsed = movie.getEpisodes()
                if (parsed.isNotEmpty()) parsed else listOf(
                    EpisodeData(seasonNumber = 1, episodeNumber = 1, title = "Capítulo 1", videoUrl = movie.videoUrl)
                )
            } else emptyList()
        )
    }

    val genres = listOf("Acción", "Comedia", "Drama", "Sci-Fi", "Fantasía", "Terror", "Documental", "Animación")
    var genreExpanded by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF09111E),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00A8E1).copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color(0xFF00A8E1),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSeries) "Editar Serie: ${movie.title}" else "Editar Película: ${movie.title}",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.LightGray)
                    }
                }

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Scrollable form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00A8E1),
                            unfocusedBorderColor = Color.Gray
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = posterUrl,
                        onValueChange = { posterUrl = it },
                        label = { Text("URL de Portada", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00A8E1),
                            unfocusedBorderColor = Color.Gray
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )

                    if (!isSeries) {
                        // Movie video link
                        OutlinedTextField(
                            value = movieVideoUrl,
                            onValueChange = { movieVideoUrl = it },
                            label = { Text("Enlace de Vídeo de la Película (Google Drive o WordPress)", color = Color.Gray) },
                            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF00A8E1)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00A8E1),
                                unfocusedBorderColor = Color.Gray
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        )
                    } else {
                        // SERIES: SEASONS & CHAPTERS MANAGEMENT
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1E30)),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00A8E1).copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = Color(0xFF00A8E1), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Temporadas y Capítulos", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            val maxSeason = seriesEpisodes.maxOfOrNull { it.seasonNumber } ?: 0
                                            val nextSeason = maxSeason + 1
                                            seriesEpisodes = seriesEpisodes + EpisodeData(
                                                seasonNumber = nextSeason,
                                                episodeNumber = 1,
                                                title = "Capítulo 1",
                                                videoUrl = ""
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2BAD3B)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ Añadir Temporada", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                val seasonsList = seriesEpisodes.map { it.seasonNumber }.distinct().sorted()

                                seasonsList.forEach { sNum ->
                                    val epsInSeason = seriesEpisodes.filter { it.seasonNumber == sNum }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF14243B),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Temporada $sNum (${epsInSeason.size} caps)", color = Color(0xFF00A8E1), fontWeight = FontWeight.Bold, fontSize = 13.sp)

                                                TextButton(
                                                    onClick = {
                                                        val nextEp = (epsInSeason.maxOfOrNull { it.episodeNumber } ?: 0) + 1
                                                        seriesEpisodes = seriesEpisodes + EpisodeData(
                                                            seasonNumber = sNum,
                                                            episodeNumber = nextEp,
                                                            title = "Capítulo $nextEp",
                                                            videoUrl = ""
                                                        )
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF00A8E1), modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text("+ Capítulo", color = Color(0xFF00A8E1), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            epsInSeason.forEach { ep ->
                                                val epIndex = seriesEpisodes.indexOf(ep)
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFF0F1E36),
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                                ) {
                                                    Column(modifier = Modifier.padding(8.dp)) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Text("Capítulo ${ep.episodeNumber}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                                                            if (seriesEpisodes.size > 1) {
                                                                IconButton(
                                                                    onClick = {
                                                                        seriesEpisodes = seriesEpisodes.toMutableList().also { it.removeAt(epIndex) }
                                                                    },
                                                                    modifier = Modifier.size(20.dp)
                                                                ) {
                                                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(14.dp))
                                                                }
                                                            }
                                                        }

                                                        OutlinedTextField(
                                                            value = ep.title,
                                                            onValueChange = { newT ->
                                                                seriesEpisodes = seriesEpisodes.toMutableList().also {
                                                                    it[epIndex] = ep.copy(title = newT)
                                                                }
                                                            },
                                                            label = { Text("Título del capítulo", color = Color.Gray, fontSize = 10.sp) },
                                                            colors = OutlinedTextFieldDefaults.colors(
                                                                focusedTextColor = Color.White,
                                                                unfocusedTextColor = Color.White,
                                                                focusedBorderColor = Color(0xFF00A8E1),
                                                                unfocusedBorderColor = Color.Gray
                                                            ),
                                                            singleLine = true,
                                                            modifier = Modifier.fillMaxWidth()
                                                        )

                                                        Spacer(modifier = Modifier.height(4.dp))

                                                        OutlinedTextField(
                                                            value = ep.videoUrl,
                                                            onValueChange = { newUrl ->
                                                                seriesEpisodes = seriesEpisodes.toMutableList().also {
                                                                    it[epIndex] = ep.copy(videoUrl = newUrl.trim())
                                                                }
                                                            },
                                                            label = { Text("Enlace de vídeo del capítulo (Google Drive / WordPress)", color = Color(0xFFFF9900), fontSize = 10.sp) },
                                                            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFFFF9900), modifier = Modifier.size(16.dp)) },
                                                            colors = OutlinedTextFieldDefaults.colors(
                                                                focusedTextColor = Color.White,
                                                                unfocusedTextColor = Color.White,
                                                                focusedBorderColor = Color(0xFFFF9900),
                                                                unfocusedBorderColor = Color.Gray
                                                            ),
                                                            singleLine = true,
                                                            modifier = Modifier.fillMaxWidth()
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

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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

                        OutlinedTextField(
                            value = duration,
                            onValueChange = { duration = it },
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
                    }

                    // Genre
                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
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
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00A8E1),
                            unfocusedBorderColor = Color.Gray
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Sinopsis", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00A8E1),
                            unfocusedBorderColor = Color.Gray
                        ),
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )
                }

                // Footer action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar", color = Color.LightGray)
                    }

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                Toast.makeText(context, "El título no puede estar vacío", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val updatedEpisodesJson = if (isSeries) {
                                EpisodeData.listToJson(seriesEpisodes)
                            } else movie.episodesJson

                            val fallbackVideo = if (isSeries) {
                                seriesEpisodes.firstOrNull { it.videoUrl.isNotBlank() }?.videoUrl ?: movie.videoUrl
                            } else movieVideoUrl

                            val updated = movie.copy(
                                title = title.trim(),
                                description = description.trim(),
                                posterUrl = posterUrl.trim(),
                                videoUrl = fallbackVideo,
                                genre = genre,
                                year = year,
                                duration = duration,
                                cast = cast.trim(),
                                episodesJson = updatedEpisodesJson
                            )

                            viewModel.updateMovie(updated)
                            Toast.makeText(context, "✓ Contenido actualizado con éxito", Toast.LENGTH_SHORT).show()
                            onUpdated(updated)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A8E1)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Guardar Cambios", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
