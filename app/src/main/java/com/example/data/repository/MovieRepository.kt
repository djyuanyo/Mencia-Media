package com.example.data.repository

import com.example.data.local.MovieDao
import com.example.data.model.Movie
import com.example.data.model.PlaybackProgress
import com.example.data.model.Profile
import com.example.data.model.UserAccount
import com.example.data.model.Watchlist
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MovieRepository(private val movieDao: MovieDao) {

    val allProfiles: Flow<List<Profile>> = movieDao.getAllProfiles()
    val allMovies: Flow<List<Movie>> = movieDao.getAllMovies()

    fun getMovieById(id: Int): Flow<Movie?> = movieDao.getMovieById(id)
    suspend fun getMovieByIdDirect(id: Int): Movie? = movieDao.getMovieByIdDirect(id)

    suspend fun insertProfile(profile: Profile): Long = movieDao.insertProfile(profile)
    suspend fun deleteProfile(profile: Profile) = movieDao.deleteProfile(profile)
    suspend fun getProfileById(id: Int): Profile? = movieDao.getProfileById(id)

    suspend fun insertMovie(movie: Movie): Long {
        val id = movieDao.insertMovie(movie)
        // Automatically publish to shared cloud catalog so all other users and devices see it
        CoroutineScope(Dispatchers.IO).launch {
            try {
                CloudCatalogService.publishMovieToCloud(movie)
            } catch (_: Exception) {}
        }
        return id
    }

    suspend fun syncWithCloud(): Int {
        return try {
            val cloudMovies = CloudCatalogService.fetchGlobalCatalog()
            if (cloudMovies.isEmpty()) return 0
            var addedCount = 0
            val localMovies = allMovies.first()
            for (remoteMovie in cloudMovies) {
                val existing = localMovies.find {
                    it.title.equals(remoteMovie.title, ignoreCase = true) &&
                            (it.year == remoteMovie.year || it.videoUrl == remoteMovie.videoUrl)
                }
                if (existing == null) {
                    movieDao.insertMovie(remoteMovie)
                    addedCount++
                } else if (existing.videoUrl.isBlank() && remoteMovie.videoUrl.isNotBlank()) {
                    movieDao.insertMovie(existing.copy(videoUrl = remoteMovie.videoUrl))
                    addedCount++
                }
            }
            addedCount
        } catch (_: Exception) {
            0
        }
    }
    suspend fun deleteMovie(movie: Movie) = movieDao.deleteMovie(movie)
    suspend fun updateMovie(movie: Movie) = movieDao.updateMovie(movie)

    // --- USER ACCOUNTS & AUTHENTICATION ---
    val allUsers: Flow<List<UserAccount>> = movieDao.getAllUsers()

    suspend fun getUserByEmail(email: String): UserAccount? = movieDao.getUserByEmailDirect(email.trim())

    suspend fun authenticateUser(email: String, password: String): UserAccount? {
        val cleanEmail = email.trim()
        val cleanPassword = password.trim()
        // Check fixed admin credentials as requested
        if (cleanEmail.equals("juanjocarrillo7@gmail.com", ignoreCase = true) && cleanPassword == "Menciano15") {
            var admin = movieDao.getUserByEmailDirect("juanjocarrillo7@gmail.com")
            if (admin == null) {
                val adminUser = UserAccount(
                    email = "juanjocarrillo7@gmail.com",
                    password = "Menciano15",
                    name = "Juan José (Admin)",
                    isAdmin = true
                )
                val id = movieDao.insertUser(adminUser)
                admin = adminUser.copy(id = id.toInt())
            }
            return admin
        }
        return movieDao.authenticateUser(cleanEmail, cleanPassword)
    }

    suspend fun registerUser(email: String, password: String, name: String): UserAccount {
        val cleanEmail = email.trim()
        val cleanPassword = password.trim()
        val cleanName = name.trim().ifEmpty { cleanEmail.substringBefore("@") }
        val isAdmin = cleanEmail.equals("juanjocarrillo7@gmail.com", ignoreCase = true)

        val existing = movieDao.getUserByEmailDirect(cleanEmail)
        if (existing != null) {
            throw IllegalArgumentException("Ya existe una cuenta con el correo $cleanEmail")
        }

        val newUser = UserAccount(
            email = cleanEmail,
            password = cleanPassword,
            name = cleanName,
            isAdmin = isAdmin
        )
        val id = movieDao.insertUser(newUser)
        return newUser.copy(id = id.toInt())
    }

    fun getWatchlistForProfile(profileId: Int): Flow<List<Movie>> = movieDao.getWatchlistForProfile(profileId)
    fun isInWatchlist(profileId: Int, movieId: Int): Flow<Boolean> = movieDao.isInWatchlist(profileId, movieId)
    
    suspend fun setInWatchlist(profileId: Int, movieId: Int, inWatchlist: Boolean) {
        if (inWatchlist) {
            movieDao.insertWatchlist(Watchlist(profileId = profileId, movieId = movieId))
        } else {
            movieDao.deleteFromWatchlist(profileId, movieId)
        }
    }

    fun getPlaybackProgress(profileId: Int, movieId: Int): Flow<PlaybackProgress?> = movieDao.getPlaybackProgress(profileId, movieId)
    fun getPlaybackProgressForProfile(profileId: Int): Flow<List<PlaybackProgress>> = movieDao.getPlaybackProgressForProfile(profileId)
    
    suspend fun savePlaybackProgress(
        profileId: Int,
        movieId: Int,
        progressMs: Long,
        durationMs: Long,
        episodeIndex: Int = 0,
        episodeNumber: Int = 1,
        seasonNumber: Int = 1,
        episodeTitle: String = ""
    ) {
        val progress = PlaybackProgress(
            profileId = profileId,
            movieId = movieId,
            progressMs = progressMs,
            durationMs = durationMs,
            lastAccessed = System.currentTimeMillis(),
            episodeIndex = episodeIndex,
            episodeNumber = episodeNumber,
            seasonNumber = seasonNumber,
            episodeTitle = episodeTitle
        )
        movieDao.insertPlaybackProgress(progress)
    }

    suspend fun getPlaybackProgressDirect(profileId: Int, movieId: Int): PlaybackProgress? = movieDao.getPlaybackProgressDirect(profileId, movieId)
    suspend fun deletePlaybackProgress(profileId: Int, movieId: Int) = movieDao.deletePlaybackProgress(profileId, movieId)

    fun getContinueWatching(profileId: Int): Flow<List<Movie>> = movieDao.getContinueWatchingMovies(profileId)

    suspend fun prepopulateIfNeeded() {
        // Pre-seed official admin user
        val existingAdmin = movieDao.getUserByEmailDirect("juanjocarrillo7@gmail.com")
        if (existingAdmin == null) {
            movieDao.insertUser(
                UserAccount(
                    email = "juanjocarrillo7@gmail.com",
                    password = "Menciano15",
                    name = "Juan José (Admin)",
                    isAdmin = true
                )
            )
        }

        val existingProfiles = allProfiles.first()
        if (existingProfiles.isEmpty()) {
            movieDao.insertProfile(Profile(name = "Juan (Admin)", avatarColorIndex = 0, isKid = false))
            movieDao.insertProfile(Profile(name = "Mamá", avatarColorIndex = 1, isKid = false))
            movieDao.insertProfile(Profile(name = "Invitado", avatarColorIndex = 2, isKid = false))
            movieDao.insertProfile(Profile(name = "Niños", avatarColorIndex = 3, isKid = true))
        }

        // 1. Synchronize with global cloud catalog to fetch titles published by any user/admin
        try {
            syncWithCloud()
        } catch (_: Exception) {}

        val existingMovies = allMovies.first()
        if (existingMovies.isEmpty()) {
            val sampleMovies = listOf(
                Movie(
                    title = "Sintel",
                    description = "La tierna historia de una chica que rescata a un pequeño dragón. Cuando es secuestrado por una bestia colosal, Sintel se embarca en un viaje épico lleno de peligros, sacrificios y aventuras visuales espectaculares.",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                    posterUrl = "https://images.unsplash.com/photo-1547483238-f400e65ccd56?w=800&q=80",
                    category = "Películas",
                    genre = "Fantasía",
                    year = "2010",
                    duration = "14 min",
                    isFeatured = true,
                    cast = "Halina Reijn, Thom Hoffman",
                    imdbRating = "8.2",
                    imdbId = "tt1727587",
                    tmdbId = "45745",
                    metadataSource = "TMDB + IMDb"
                ),
                Movie(
                    title = "Tears of Steel",
                    description = "Cortometraje de ciencia ficción que aborda un mundo distópico en Londres. Un grupo de científicos intenta salvar el planeta del azote de robots descontrolados usando tecnologías y conexiones del pasado.",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                    posterUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=800&q=80",
                    category = "Películas",
                    genre = "Sci-Fi",
                    year = "2012",
                    duration = "12 min",
                    isFeatured = true,
                    cast = "Derek de Lint, Sergio Hasselbaink, Denise Rebergen",
                    imdbRating = "7.8",
                    imdbId = "tt2426860",
                    tmdbId = "131154",
                    metadataSource = "TMDB + IMDb"
                ),
                Movie(
                    title = "Big Buck Bunny",
                    description = "La vida de un pacífico conejo gigante da un giro cuando tres traviesas ardillas deciden burlarse de él y estropear su entorno natural. Es hora de darles una lección cómica con trampas del bosque.",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    posterUrl = "https://images.unsplash.com/photo-1501854140801-50d01698950b?w=800&q=80",
                    category = "Películas",
                    genre = "Comedia",
                    year = "2008",
                    duration = "10 min",
                    isFeatured = false,
                    cast = "Personajes Animados de la Fundación Blender",
                    imdbRating = "8.0",
                    imdbId = "tt1254207",
                    tmdbId = "10378",
                    metadataSource = "TMDB + IMDb"
                ),
                Movie(
                    title = "Cosmos: Serie Documental",
                    description = "Una travesía monumental a través del espacio y el tiempo. Exploración científica de las leyes cósmicas, el origen de la vida y el futuro de la humanidad en el universo observable.",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    posterUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=800&q=80",
                    category = "Series",
                    genre = "Documental",
                    year = "2020",
                    duration = "3 episodios",
                    isFeatured = false,
                    cast = "Neil deGrasse Tyson, Ann Druyan, Seth MacFarlane",
                    imdbRating = "9.3",
                    imdbId = "tt2395695",
                    tmdbId = "58474",
                    episodesJson = "[{\"seasonNumber\":1,\"episodeNumber\":1,\"title\":\"Hacia las estrellas\",\"overview\":\"Un viaje al borde del universo conocido comenzando en las playas de Alejandría.\",\"runtime\":\"45 min\"},{\"seasonNumber\":1,\"episodeNumber\":2,\"title\":\"El río del tiempo y el ADN\",\"overview\":\"Descubriendo el código molecular que conecta toda la vida terrenal con las supernovas.\",\"runtime\":\"48 min\"},{\"seasonNumber\":1,\"episodeNumber\":3,\"title\":\"Mundos perdidos y encontrados\",\"overview\":\"Los secretos geológicos ocultos de los exoplanetas en la Vía Láctea.\",\"runtime\":\"46 min\"}]",
                    metadataSource = "TMDB + TheTVDB + IMDb"
                ),
                Movie(
                    title = "WordPress Showcase",
                    description = "Demostración de streaming fluido en la nube, optimizado para reproducción directa y metadatos dinámicos.",
                    videoUrl = "https://vids.wordpress.com/videos/v9_video.mp4",
                    posterUrl = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=800&q=80",
                    category = "Películas",
                    genre = "Documental",
                    year = "2024",
                    duration = "5 min",
                    isFeatured = false,
                    cast = "Equipo de Producción WordPress Streaming",
                    imdbRating = "7.5",
                    imdbId = "",
                    tmdbId = "",
                    metadataSource = "TMDB"
                )
            )
            for (movie in sampleMovies) {
                movieDao.insertMovie(movie)
            }
        }
    }
}
