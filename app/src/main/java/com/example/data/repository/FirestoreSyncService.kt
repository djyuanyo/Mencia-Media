package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.Movie
import com.example.data.model.PlaybackProgress
import com.example.data.model.Profile
import com.example.data.model.UserAccount
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class RemoteProgressRecord(
    val progress: PlaybackProgress,
    val movieTitle: String = "",
    val movieYear: String = "",
    val userEmail: String = "",
    val profileName: String = ""
)

class FirestoreSyncService(context: Context) {

    private val tag = "FirestoreSyncService"
    private val appContext = context.applicationContext

    private val firestore: FirebaseFirestore by lazy {
        try {
            if (FirebaseApp.getApps(appContext).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:612955457767:android:78b09451dd1c8d03c0fa42")
                    .setApiKey("AIzaSyCC5b-mGxMAWJ6XiiD4PkXGC7LA4jx-tgY")
                    .setProjectId("gen-lang-client-0763337447")
                    .setStorageBucket("gen-lang-client-0763337447.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(appContext, options)
            }
        } catch (e: Exception) {
            Log.e(tag, "FirebaseApp init: ${e.message}")
        }
        val dbId = appContext.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(dbId)
    }

    // -------------------------------------------------------------
    // 1. PELÍCULAS Y SERIES (Movies & Series Catalog)
    // -------------------------------------------------------------
    suspend fun saveMovie(movie: Movie) = withContext(Dispatchers.IO) {
        try {
            val docId = if (movie.id > 0) movie.id.toString() else "${movie.title.trim().lowercase()}_${movie.year.trim()}"
            val data = hashMapOf<String, Any>(
                "id" to movie.id,
                "title" to movie.title,
                "description" to movie.description,
                "videoUrl" to movie.videoUrl,
                "posterUrl" to movie.posterUrl,
                "category" to movie.category,
                "genre" to movie.genre,
                "year" to movie.year,
                "duration" to movie.duration,
                "isFeatured" to movie.isFeatured,
                "cast" to movie.cast,
                "imdbRating" to movie.imdbRating,
                "imdbId" to movie.imdbId,
                "tmdbId" to movie.tmdbId,
                "episodesJson" to movie.episodesJson,
                "trailerUrl" to movie.trailerUrl,
                "metadataSource" to movie.metadataSource,
                "addedAt" to movie.addedAt
            )
            firestore.collection("movies").document(docId).set(data, SetOptions.merge()).await()
            Log.d(tag, "✓ Película/Serie guardada en Firestore: ${movie.title} (ID: $docId)")
        } catch (e: Exception) {
            Log.e(tag, "Error guardando película en Firestore: ${e.message}", e)
        }
    }

    suspend fun deleteMovie(movieId: Int, title: String = "", year: String = "") = withContext(Dispatchers.IO) {
        try {
            if (movieId > 0) {
                firestore.collection("movies").document(movieId.toString()).delete().await()
            }
            if (title.isNotBlank()) {
                val altDocId = "${title.trim().lowercase()}_${year.trim()}"
                firestore.collection("movies").document(altDocId).delete().await()
            }
            Log.d(tag, "✓ Película eliminada de Firestore: ID $movieId")
        } catch (e: Exception) {
            Log.e(tag, "Error eliminando película de Firestore: ${e.message}", e)
        }
    }

    suspend fun fetchMovies(): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("movies").get().await()
            val list = mutableListOf<Movie>()
            for (doc in snapshot.documents) {
                val title = doc.getString("title") ?: continue
                val id = (doc.getLong("id") ?: 0L).toInt()
                val movie = Movie(
                    id = id,
                    title = title,
                    description = doc.getString("description") ?: "",
                    videoUrl = doc.getString("videoUrl") ?: "",
                    posterUrl = doc.getString("posterUrl") ?: "",
                    category = doc.getString("category") ?: "Películas",
                    genre = doc.getString("genre") ?: "Acción",
                    year = doc.getString("year") ?: "2024",
                    duration = doc.getString("duration") ?: "120 min",
                    isFeatured = doc.getBoolean("isFeatured") ?: false,
                    cast = doc.getString("cast") ?: "",
                    imdbRating = doc.getString("imdbRating") ?: "",
                    imdbId = doc.getString("imdbId") ?: "",
                    tmdbId = doc.getString("tmdbId") ?: "",
                    episodesJson = doc.getString("episodesJson") ?: "",
                    trailerUrl = doc.getString("trailerUrl") ?: "",
                    metadataSource = doc.getString("metadataSource") ?: "Firestore Sync",
                    addedAt = doc.getLong("addedAt") ?: System.currentTimeMillis()
                )
                list.add(movie)
            }
            Log.d(tag, "✓ Obtenidas ${list.size} películas/series desde Firestore")
            list
        } catch (e: Exception) {
            Log.e(tag, "Error descargando películas de Firestore: ${e.message}", e)
            emptyList()
        }
    }

    // -------------------------------------------------------------
    // 2. USUARIOS REGISTRADOS Y PERFILES (User Accounts & Profiles)
    // -------------------------------------------------------------
    suspend fun saveUser(user: UserAccount) = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = user.email.trim().lowercase()
            val docId = cleanEmail.replace(".", "_")
            val data = hashMapOf<String, Any>(
                "id" to user.id,
                "email" to user.email,
                "password" to user.password,
                "name" to user.name,
                "isAdmin" to user.isAdmin,
                "isApproved" to user.isApproved,
                "createdAt" to user.createdAt
            )
            firestore.collection("users").document(docId).set(data, SetOptions.merge()).await()
            Log.d(tag, "✓ Usuario guardado en Firestore: ${user.email} (Aprobado: ${user.isApproved})")
        } catch (e: Exception) {
            Log.e(tag, "Error guardando usuario en Firestore: ${e.message}", e)
        }
    }

    suspend fun deleteUser(email: String) = withContext(Dispatchers.IO) {
        try {
            val docId = email.trim().lowercase().replace(".", "_")
            firestore.collection("users").document(docId).delete().await()
            Log.d(tag, "✓ Usuario eliminado de Firestore: $email")
        } catch (e: Exception) {
            Log.e(tag, "Error eliminando usuario de Firestore: ${e.message}", e)
        }
    }

    suspend fun fetchUsers(): List<UserAccount> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("users").get().await()
            val users = mutableListOf<UserAccount>()
            for (doc in snapshot.documents) {
                val email = doc.getString("email") ?: continue
                val user = UserAccount(
                    id = (doc.getLong("id") ?: 0L).toInt(),
                    email = email,
                    password = doc.getString("password") ?: "",
                    name = doc.getString("name") ?: email.substringBefore("@"),
                    isAdmin = doc.getBoolean("isAdmin") ?: false,
                    isApproved = doc.getBoolean("isApproved") ?: false,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
                users.add(user)
            }
            Log.d(tag, "✓ Obtenidos ${users.size} usuarios desde Firestore")
            users
        } catch (e: Exception) {
            Log.e(tag, "Error descargando usuarios de Firestore: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun saveProfile(profile: Profile) = withContext(Dispatchers.IO) {
        try {
            val docId = "${profile.userId}_${profile.id}_${profile.name.trim().lowercase()}"
            val data = hashMapOf<String, Any>(
                "id" to profile.id,
                "userId" to profile.userId,
                "name" to profile.name,
                "avatarColorIndex" to profile.avatarColorIndex,
                "isKid" to profile.isKid,
                "createdAt" to profile.createdAt
            )
            firestore.collection("profiles").document(docId).set(data, SetOptions.merge()).await()
            Log.d(tag, "✓ Perfil guardado en Firestore: ${profile.name}")
        } catch (e: Exception) {
            Log.e(tag, "Error guardando perfil en Firestore: ${e.message}", e)
        }
    }

    suspend fun fetchProfilesForUser(userId: Int): List<Profile> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("profiles")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val profiles = mutableListOf<Profile>()
            for (doc in snapshot.documents) {
                val name = doc.getString("name") ?: continue
                profiles.add(
                    Profile(
                        id = (doc.getLong("id") ?: 0L).toInt(),
                        userId = userId,
                        name = name,
                        avatarColorIndex = (doc.getLong("avatarColorIndex") ?: 0L).toInt(),
                        isKid = doc.getBoolean("isKid") ?: false,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                )
            }
            profiles
        } catch (e: Exception) {
            Log.e(tag, "Error descargando perfiles de Firestore: ${e.message}", e)
            emptyList()
        }
    }

    // -------------------------------------------------------------
    // 3. HISTORIAL DE REPRODUCCIÓN, CAPÍTULOS Y ESTADO DE "VISTO"
    // -------------------------------------------------------------
    suspend fun savePlaybackProgress(
        progress: PlaybackProgress,
        movieTitle: String = "",
        movieYear: String = "",
        userEmail: String = "",
        profileName: String = ""
    ) = withContext(Dispatchers.IO) {
        try {
            val docId = if (userEmail.isNotBlank() && profileName.isNotBlank() && movieTitle.isNotBlank()) {
                val cleanEmail = userEmail.trim().lowercase().replace(".", "_")
                val cleanProfile = profileName.trim().lowercase().replace(" ", "_")
                val cleanMovie = movieTitle.trim().lowercase().replace(" ", "_").replace("/", "_")
                "${cleanEmail}__${cleanProfile}__${cleanMovie}"
            } else {
                "${progress.profileId}_${progress.movieId}"
            }
            val isCompleted = progress.isCompleted || (progress.durationMs > 0 && progress.progressMs >= (progress.durationMs * 9L / 10L))
            val data = hashMapOf<String, Any>(
                "profileId" to progress.profileId,
                "movieId" to progress.movieId,
                "movieTitle" to movieTitle,
                "movieYear" to movieYear,
                "userEmail" to userEmail,
                "profileName" to profileName,
                "progressMs" to progress.progressMs,
                "durationMs" to progress.durationMs,
                "lastAccessed" to progress.lastAccessed,
                "episodeIndex" to progress.episodeIndex,
                "episodeNumber" to progress.episodeNumber,
                "seasonNumber" to progress.seasonNumber,
                "episodeTitle" to progress.episodeTitle,
                "isCompleted" to isCompleted
            )
            firestore.collection("playback_progress").document(docId).set(data, SetOptions.merge()).await()
            if (docId != "${progress.profileId}_${progress.movieId}") {
                firestore.collection("playback_progress").document("${progress.profileId}_${progress.movieId}").set(data, SetOptions.merge()).await()
            }
            Log.d(tag, "✓ Progreso guardado en Firestore: ${movieTitle.ifEmpty { "Película ${progress.movieId}" }} (${progress.formatProgressTime()}, Visto: $isCompleted)")
        } catch (e: Exception) {
            Log.e(tag, "Error guardando progreso en Firestore: ${e.message}", e)
        }
    }

    suspend fun deletePlaybackProgress(
        profileId: Int,
        movieId: Int,
        userEmail: String = "",
        profileName: String = "",
        movieTitle: String = ""
    ) = withContext(Dispatchers.IO) {
        try {
            val docId = "${profileId}_${movieId}"
            firestore.collection("playback_progress").document(docId).delete().await()
            if (userEmail.isNotBlank() && profileName.isNotBlank() && movieTitle.isNotBlank()) {
                val cleanEmail = userEmail.trim().lowercase().replace(".", "_")
                val cleanProfile = profileName.trim().lowercase().replace(" ", "_")
                val cleanMovie = movieTitle.trim().lowercase().replace(" ", "_").replace("/", "_")
                firestore.collection("playback_progress").document("${cleanEmail}__${cleanProfile}__${cleanMovie}").delete().await()
            }
            Log.d(tag, "✓ Progreso borrado de Firestore: Perfil $profileId, Película $movieId")
        } catch (e: Exception) {
            Log.e(tag, "Error borrando progreso de Firestore: ${e.message}", e)
        }
    }

    suspend fun fetchPlaybackProgressRecords(
        profileId: Int,
        userEmail: String = "",
        profileName: String = ""
    ): List<RemoteProgressRecord> = withContext(Dispatchers.IO) {
        try {
            val list = mutableListOf<RemoteProgressRecord>()
            
            // Query 1: by profileId
            val snapProfile = firestore.collection("playback_progress")
                .whereEqualTo("profileId", profileId)
                .get()
                .await()
            
            for (doc in snapProfile.documents) {
                val movieId = (doc.getLong("movieId") ?: 0L).toInt()
                val progressMs = doc.getLong("progressMs") ?: 0L
                val durationMs = doc.getLong("durationMs") ?: 0L
                val isCompleted = doc.getBoolean("isCompleted") ?: (durationMs > 0 && progressMs >= (durationMs * 9L / 10L))

                val prog = PlaybackProgress(
                    profileId = profileId,
                    movieId = movieId,
                    progressMs = progressMs,
                    durationMs = durationMs,
                    lastAccessed = doc.getLong("lastAccessed") ?: System.currentTimeMillis(),
                    episodeIndex = (doc.getLong("episodeIndex") ?: 0L).toInt(),
                    episodeNumber = (doc.getLong("episodeNumber") ?: 1L).toInt(),
                    seasonNumber = (doc.getLong("seasonNumber") ?: 1L).toInt(),
                    episodeTitle = doc.getString("episodeTitle") ?: "",
                    isCompleted = isCompleted
                )
                list.add(
                    RemoteProgressRecord(
                        progress = prog,
                        movieTitle = doc.getString("movieTitle") ?: "",
                        movieYear = doc.getString("movieYear") ?: "",
                        userEmail = doc.getString("userEmail") ?: "",
                        profileName = doc.getString("profileName") ?: ""
                    )
                )
            }

            // Query 2: by userEmail if specified
            if (userEmail.isNotBlank()) {
                val snapUser = firestore.collection("playback_progress")
                    .whereEqualTo("userEmail", userEmail)
                    .get()
                    .await()
                
                for (doc in snapUser.documents) {
                    val pName = doc.getString("profileName") ?: ""
                    if (profileName.isNotBlank() && pName.isNotBlank() && !pName.equals(profileName, ignoreCase = true)) {
                        continue
                    }
                    val movieId = (doc.getLong("movieId") ?: 0L).toInt()
                    val progressMs = doc.getLong("progressMs") ?: 0L
                    val durationMs = doc.getLong("durationMs") ?: 0L
                    val isCompleted = doc.getBoolean("isCompleted") ?: (durationMs > 0 && progressMs >= (durationMs * 9L / 10L))
                    val movieTitle = doc.getString("movieTitle") ?: ""

                    // Avoid duplicate if already fetched
                    if (list.none { it.movieTitle.equals(movieTitle, ignoreCase = true) || (it.progress.movieId == movieId && movieId > 0) }) {
                        val prog = PlaybackProgress(
                            profileId = profileId,
                            movieId = movieId,
                            progressMs = progressMs,
                            durationMs = durationMs,
                            lastAccessed = doc.getLong("lastAccessed") ?: System.currentTimeMillis(),
                            episodeIndex = (doc.getLong("episodeIndex") ?: 0L).toInt(),
                            episodeNumber = (doc.getLong("episodeNumber") ?: 1L).toInt(),
                            seasonNumber = (doc.getLong("seasonNumber") ?: 1L).toInt(),
                            episodeTitle = doc.getString("episodeTitle") ?: "",
                            isCompleted = isCompleted
                        )
                        list.add(
                            RemoteProgressRecord(
                                progress = prog,
                                movieTitle = movieTitle,
                                movieYear = doc.getString("movieYear") ?: "",
                                userEmail = userEmail,
                                profileName = pName
                            )
                        )
                    }
                }
            }

            Log.d(tag, "✓ Obtenidos ${list.size} registros de progreso e historial desde Firestore")
            list
        } catch (e: Exception) {
            Log.e(tag, "Error descargando progreso de Firestore: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun fetchPlaybackProgressForProfile(profileId: Int): List<PlaybackProgress> = withContext(Dispatchers.IO) {
        fetchPlaybackProgressRecords(profileId).map { it.progress }
    }
}
