package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val avatarColorIndex: Int = 0, // Index representing selected profile avatar color
    val isKid: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class EpisodeData(
    val seasonNumber: Int = 1,
    val episodeNumber: Int = 1,
    val title: String = "",
    val overview: String = "",
    val runtime: String = "45 min",
    val stillUrl: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("seasonNumber", seasonNumber)
        put("episodeNumber", episodeNumber)
        put("title", title)
        put("overview", overview)
        put("runtime", runtime)
        put("stillUrl", stillUrl)
    }

    companion object {
        fun fromJson(json: JSONObject): EpisodeData = EpisodeData(
            seasonNumber = json.optInt("seasonNumber", 1),
            episodeNumber = json.optInt("episodeNumber", 1),
            title = json.optString("title", ""),
            overview = json.optString("overview", ""),
            runtime = json.optString("runtime", "45 min"),
            stillUrl = json.optString("stillUrl", "")
        )

        fun listToJson(list: List<EpisodeData>): String {
            val array = JSONArray()
            list.forEach { array.put(it.toJson()) }
            return array.toString()
        }

        fun listFromJson(jsonStr: String): List<EpisodeData> {
            if (jsonStr.isBlank()) return emptyList()
            return try {
                val array = JSONArray(jsonStr)
                val result = mutableListOf<EpisodeData>()
                for (i in 0 until array.length()) {
                    result.add(fromJson(array.getJSONObject(i)))
                }
                result
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}

@Entity(tableName = "movies")
data class Movie(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val videoUrl: String, // WordPress or Google Drive link
    val posterUrl: String = "", // Custom or placeholder picture
    val category: String = "Películas", // e.g. "Películas", "Series"
    val genre: String = "Acción", // e.g. "Acción", "Comedia", "Drama", "Ciencia Ficción", "Terror"
    val year: String = "2024",
    val duration: String = "120 min",
    val isFeatured: Boolean = false,
    val cast: String = "", // Actors / Actores like Plex
    val imdbRating: String = "", // e.g. "8.8" (de IMDb)
    val imdbId: String = "", // e.g. "tt1375666" (de IMDb)
    val tmdbId: String = "", // e.g. "27205" (de The Movie Database)
    val episodesJson: String = "", // Serialized EpisodeData list (Orden de TheTVDB)
    val metadataSource: String = "TMDB + IMDb + TheTVDB",
    val addedAt: Long = System.currentTimeMillis()
) {
    fun getEpisodes(): List<EpisodeData> = EpisodeData.listFromJson(episodesJson)

    fun getImdbScoreFormatted(): String {
        val r = imdbRating.trim()
        if (r.isEmpty()) return ""
        return "$r / 10"
    }

    fun getImdbScorePercentage(): Int {
        val r = imdbRating.toDoubleOrNull() ?: return 0
        return (r * 10).toInt().coerceIn(0, 100)
    }

    fun getImdbScoreLabel(): String {
        val r = imdbRating.toDoubleOrNull() ?: return ""
        return when {
            r >= 9.0 -> "Obra Maestra Universal"
            r >= 8.5 -> "Top IMDb • Excelente"
            r >= 7.8 -> "Aclamada por la Crítica"
            r >= 7.0 -> "Muy Buena Valoración"
            r >= 6.0 -> "Buena Valoración"
            r >= 5.0 -> "Valoración Media"
            else -> "Regular"
        }
    }

    fun getImdbUrl(): String {
        return if (imdbId.startsWith("tt")) "https://www.imdb.com/title/$imdbId" else ""
    }
}

@Entity(tableName = "watchlist", primaryKeys = ["profileId", "movieId"])
data class Watchlist(
    val profileId: Int,
    val movieId: Int,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playback_progress", primaryKeys = ["profileId", "movieId"])
data class PlaybackProgress(
    val profileId: Int,
    val movieId: Int,
    val progressMs: Long,
    val durationMs: Long = 0,
    val lastAccessed: Long = System.currentTimeMillis(),
    val episodeIndex: Int = 0,
    val episodeNumber: Int = 1,
    val seasonNumber: Int = 1,
    val episodeTitle: String = ""
) {
    fun formatProgressTime(): String {
        val totalSeconds = (progressMs / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(java.util.Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    fun formatDurationTime(): String {
        if (durationMs <= 0) return ""
        val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(java.util.Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    fun getProgressFraction(): Float {
        if (durationMs <= 0) return 0f
        return (progressMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }
}
