package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Movie

/**
 * Gold-colored compact IMDb pill used on cards and list items.
 */
@Composable
fun ImdbRatingPill(
    rating: String,
    modifier: Modifier = Modifier
) {
    if (rating.isBlank()) return

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFFF5C518), // Iconic IMDb yellow
        modifier = modifier.testTag("imdb_rating_pill")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Text(
                text = "IMDb",
                color = Color.Black,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "$rating ★",
                color = Color.Black,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

/**
 * Full IMDb ratings and scores card displayed in media details.
 * Shows official IMDb score out of 10, star breakdown, critique status, and official link.
 */
@Composable
fun ImdbScoreCard(
    movie: Movie,
    onRefreshRating: (() -> Unit)? = null,
    isRefreshing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val ratingStr = movie.imdbRating.trim()
    val ratingNum = ratingStr.toDoubleOrNull() ?: 0.0
    val scorePercentage = movie.getImdbScorePercentage()
    val scoreLabel = movie.getImdbScoreLabel()
    val imdbUrl = movie.getImdbUrl()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFF5C518).copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .testTag("imdb_score_card"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2D))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: IMDb brand & Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFF5C518)
                    ) {
                        Text(
                            text = "IMDb",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Calificación y Crítica Oficial",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (onRefreshRating != null) {
                    IconButton(
                        onClick = onRefreshRating,
                        modifier = Modifier.size(28.dp).testTag("refresh_imdb_button")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFFF5C518), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Actualizar IMDb", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Score Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large Score
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (ratingStr.isNotBlank()) ratingStr else "N/D",
                        color = Color(0xFFF5C518),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = " / 10",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Score Verdict & Stars
                Column(modifier = Modifier.weight(1f)) {
                    if (scoreLabel.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = when {
                                ratingNum >= 8.5 -> Color(0xFFF5C518).copy(alpha = 0.2f)
                                ratingNum >= 7.5 -> Color(0xFF2BAD3B).copy(alpha = 0.2f)
                                else -> Color(0xFF00A8E1).copy(alpha = 0.2f)
                            }
                        ) {
                            Text(
                                text = scoreLabel,
                                color = when {
                                    ratingNum >= 8.5 -> Color(0xFFF5C518)
                                    ratingNum >= 7.5 -> Color(0xFF4ADE80)
                                    else -> Color(0xFF00A8E1)
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Star indicators (out of 5 stars)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val starsCount = (ratingNum / 2.0).toInt().coerceIn(0, 5)
                        for (i in 1..5) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (i <= starsCount) Color(0xFFF5C518) else Color.DarkGray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (scorePercentage > 0) "$scorePercentage% de aprobación" else "Sin votos registrados",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar Visualizer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF09111E))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = (scorePercentage / 100f).coerceIn(0f, 1f))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFE5A100), Color(0xFFF5C518))
                            )
                        )
                )
            }

            // Footer Link to IMDb website
            if (imdbUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(imdbUrl))
                            context.startActivity(intent)
                        }
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Ver ficha y críticas en IMDb (${movie.imdbId}) ↗",
                        color = Color(0xFF00A8E1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
