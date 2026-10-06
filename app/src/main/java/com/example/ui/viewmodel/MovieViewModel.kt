package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Movie
import com.example.data.model.PlaybackProgress
import com.example.data.model.Profile
import com.example.data.model.UserAccount
import com.example.data.repository.ImdbDetails
import com.example.data.repository.MediaSuggestion
import com.example.data.repository.MetadataService
import com.example.data.repository.MovieRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MovieViewModel(
    private val repository: MovieRepository,
    private val metadataService: MetadataService = MetadataService()
) : ViewModel() {

    private val _isRefreshingImdb = MutableStateFlow(false)
    val isRefreshingImdb: StateFlow<Boolean> = _isRefreshingImdb.asStateFlow()

    fun fetchImdbForMovie(movieId: Int) {
        viewModelScope.launch {
            _isRefreshingImdb.value = true
            try {
                val movie = repository.getMovieByIdDirect(movieId) ?: return@launch
                val isTv = movie.category.equals("Series", ignoreCase = true)
                val imdbData = if (movie.imdbId.startsWith("tt")) {
                    metadataService.fetchImdbDetails(movie.imdbId, isTv)
                } else {
                    metadataService.fetchImdbByTitle(movie.title, isTv)
                }

                if (imdbData.rating.isNotBlank()) {
                    val updated = movie.copy(
                        imdbRating = imdbData.rating,
                        imdbId = if (movie.imdbId.isBlank()) imdbData.imdbId else movie.imdbId
                    )
                    repository.insertMovie(updated)
                }
            } catch (_: Exception) {
            } finally {
                _isRefreshingImdb.value = false
            }
        }
    }

    fun fetchImdbForDraft(
        title: String,
        imdbId: String,
        isTv: Boolean,
        onResult: (ImdbDetails) -> Unit
    ) {
        viewModelScope.launch {
            val result = if (imdbId.startsWith("tt")) {
                metadataService.fetchImdbDetails(imdbId, isTv)
            } else {
                metadataService.fetchImdbByTitle(title, isTv)
            }
            onResult(result)
        }
    }

    // Currently active user account & Administrator permission flag
    private val _currentUserAccount = MutableStateFlow<UserAccount?>(null)
    val currentUserAccount: StateFlow<UserAccount?> = _currentUserAccount.asStateFlow()

    val isAdmin: StateFlow<Boolean> = _currentUserAccount.map { user ->
        user != null && (user.isAdmin || user.email.equals("juanjocarrillo7@gmail.com", ignoreCase = true))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // All available profiles for currently active user account (each user has their own profiles)
    val profiles: StateFlow<List<Profile>> = _currentUserAccount
        .flatMapLatest { user ->
            if (user != null) {
                repository.getProfilesForUser(user.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All registered users for administrator management
    val allUsers: StateFlow<List<UserAccount>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun approveUser(userId: Int, approved: Boolean) {
        viewModelScope.launch {
            repository.setUserApproval(userId, approved)
        }
    }

    fun deleteUser(user: UserAccount) {
        viewModelScope.launch {
            repository.deleteUser(user)
        }
    }

    // Currently active/logged profile
    private val _currentProfile = MutableStateFlow<Profile?>(null)
    val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    // Flag indicating if DB populating is completed
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    // All movies catalog
    val allMovies = repository.allMovies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Plex-style automatic metadata suggestions flow
    private val _metadataSuggestions = MutableStateFlow<List<MediaSuggestion>>(emptyList())
    val metadataSuggestions: StateFlow<List<MediaSuggestion>> = _metadataSuggestions.asStateFlow()

    private val _isSearchingMetadata = MutableStateFlow(false)
    val isSearchingMetadata: StateFlow<Boolean> = _isSearchingMetadata.asStateFlow()

    fun searchMetadata(query: String) {
        val clean = query.trim()
        if (clean.isBlank()) {
            _metadataSuggestions.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearchingMetadata.value = true
            try {
                val results = metadataService.search(clean)
                _metadataSuggestions.value = results
            } catch (_: Exception) {
                _metadataSuggestions.value = emptyList()
            } finally {
                _isSearchingMetadata.value = false
            }
        }
    }

    fun clearMetadataSuggestions() {
        _metadataSuggestions.value = emptyList()
    }

    fun extractCleanTitle(input: String): String {
        return metadataService.extractTitle(input)
    }

    // Profile-specific Watchlist flow
    val watchlist: StateFlow<List<Movie>> = _currentProfile
        .flatMapLatest { profile ->
            if (profile != null) {
                repository.getWatchlistForProfile(profile.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Profile-specific "Continue Watching" flow
    val continueWatching: StateFlow<List<Movie>> = _currentProfile
        .flatMapLatest { profile ->
            if (profile != null) {
                repository.getContinueWatching(profile.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.prepopulateIfNeeded()
            _isInitialized.value = true
            // Periodically sync cloud catalog in background
            try {
                repository.syncWithCloud()
            } catch (_: Exception) {}
        }
    }

    fun syncCloudCatalog() {
        viewModelScope.launch {
            try {
                repository.syncWithCloud()
            } catch (_: Exception) {}
        }
    }

    fun selectProfile(profile: Profile?) {
        _currentProfile.value = profile
    }

    fun createProfile(name: String, avatarColorIndex: Int, isKid: Boolean) {
        val currentUserId = _currentUserAccount.value?.id ?: 0
        viewModelScope.launch {
            repository.insertProfile(
                Profile(
                    userId = currentUserId,
                    name = name,
                    avatarColorIndex = avatarColorIndex,
                    isKid = isKid
                )
            )
        }
    }

    fun deleteProfile(profile: Profile) {
        viewModelScope.launch {
            if (_currentProfile.value?.id == profile.id) {
                _currentProfile.value = null
            }
            repository.deleteProfile(profile)
        }
    }

    fun setCustomTmdbKey(key: String) {
        MetadataService.customTmdbApiKey = key.trim()
    }

    fun getCustomTmdbKey(): String = MetadataService.customTmdbApiKey

    fun addMovie(
        title: String,
        description: String,
        videoUrl: String,
        posterUrl: String,
        category: String,
        genre: String,
        year: String,
        duration: String,
        isFeatured: Boolean,
        cast: String = "",
        imdbRating: String = "",
        imdbId: String = "",
        tmdbId: String = "",
        episodesJson: String = "",
        metadataSource: String = "TMDB + IMDb + TheTVDB"
    ) {
        viewModelScope.launch {
            val finalPoster = posterUrl.trim().ifEmpty {
                // Preset images based on genre
                when (genre.lowercase()) {
                    "acción" -> "https://images.unsplash.com/photo-1547483238-f400e65ccd56?w=800&q=80"
                    "fantasía" -> "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?w=800&q=80"
                    "sci-fi" -> "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=800&q=80"
                    "comedia" -> "https://images.unsplash.com/photo-1501854140801-50d01698950b?w=800&q=80"
                    "drama" -> "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=800&q=80"
                    else -> "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=800&q=80" // general movie theater card
                }
            }
            repository.insertMovie(
                Movie(
                    title = title,
                    description = description,
                    videoUrl = videoUrl,
                    posterUrl = finalPoster,
                    category = category,
                    genre = genre,
                    year = year,
                    duration = duration,
                    isFeatured = isFeatured,
                    cast = cast,
                    imdbRating = imdbRating,
                    imdbId = imdbId,
                    tmdbId = tmdbId,
                    episodesJson = episodesJson,
                    metadataSource = metadataSource
                )
            )
        }
    }

    fun updateMovie(movie: Movie) {
        viewModelScope.launch {
            repository.updateMovie(movie)
        }
    }

    fun deleteMovie(movie: Movie) {
        viewModelScope.launch {
            repository.deleteMovie(movie)
        }
    }

    suspend fun login(email: String, password: String): Result<UserAccount> {
        return try {
            val user = repository.authenticateUser(email, password)
            if (user != null) {
                _currentUserAccount.value = user
                // Link or create a profile matching user name for their account
                val userProfiles = repository.getProfilesForUser(user.id).first()
                if (userProfiles.isNotEmpty()) {
                    val matched = userProfiles.find { it.name.equals(user.name, ignoreCase = true) }
                    _currentProfile.value = matched ?: userProfiles.first()
                } else {
                    val newProfile = Profile(userId = user.id, name = user.name, avatarColorIndex = 0, isKid = false)
                    val id = repository.insertProfile(newProfile)
                    _currentProfile.value = newProfile.copy(id = id.toInt())
                }
                Result.success(user)
            } else {
                Result.failure(Exception("Correo o contraseña incorrectos"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, password: String, name: String): Result<UserAccount> {
        return try {
            val user = repository.registerUser(email, password, name)
            // If admin or already approved, set as current user; otherwise leave unauthenticated
            if (user.isAdmin || user.isApproved) {
                _currentUserAccount.value = user
                val userProfiles = repository.getProfilesForUser(user.id).first()
                _currentProfile.value = userProfiles.firstOrNull()
            }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logoutUser() {
        _currentUserAccount.value = null
        _currentProfile.value = null
    }

    fun toggleWatchlist(movieId: Int, inWatchlist: Boolean) {
        val profile = _currentProfile.value ?: return
        viewModelScope.launch {
            repository.setInWatchlist(profile.id, movieId, inWatchlist)
        }
    }

    fun isMovieInWatchlist(movieId: Int): Flow<Boolean> {
        val profile = _currentProfile.value ?: return flowOf(false)
        return repository.isInWatchlist(profile.id, movieId)
    }

    val allPlaybackProgresses: Flow<Map<Int, PlaybackProgress>> = _currentProfile.flatMapLatest { profile ->
        if (profile == null) flowOf(emptyMap())
        else repository.getPlaybackProgressForProfile(profile.id).map { list -> list.associateBy { it.movieId } }
    }

    fun getMoviePlaybackProgress(movieId: Int): Flow<Long> {
        val profile = _currentProfile.value ?: return flowOf(0L)
        return repository.getPlaybackProgress(profile.id, movieId).map { it?.progressMs ?: 0L }
    }

    fun getMoviePlaybackProgressDetails(movieId: Int): Flow<PlaybackProgress?> {
        val profile = _currentProfile.value ?: return flowOf(null)
        return repository.getPlaybackProgress(profile.id, movieId)
    }

    fun updatePlaybackProgress(
        movieId: Int,
        progressMs: Long,
        durationMs: Long,
        episodeIndex: Int = 0,
        episodeNumber: Int = 1,
        seasonNumber: Int = 1,
        episodeTitle: String = ""
    ) {
        val profile = _currentProfile.value ?: return
        viewModelScope.launch {
            if (progressMs > 0) {
                repository.savePlaybackProgress(
                    profile.id,
                    movieId,
                    progressMs,
                    durationMs,
                    episodeIndex,
                    episodeNumber,
                    seasonNumber,
                    episodeTitle
                )
            }
        }
    }

    fun clearPlaybackProgress(movieId: Int) {
        val profile = _currentProfile.value ?: return
        viewModelScope.launch {
            repository.deletePlaybackProgress(profile.id, movieId)
        }
    }
}

// ViewModel Factory
class MovieViewModelFactory(private val repository: MovieRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MovieViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MovieViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
