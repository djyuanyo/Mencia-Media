package com.example

import com.example.data.local.MovieDao
import com.example.data.model.Movie
import com.example.data.model.PlaybackProgress
import com.example.data.model.Profile
import com.example.data.model.UserAccount
import com.example.data.model.Watchlist
import com.example.data.repository.MovieRepository
import com.example.ui.viewmodel.MovieViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleUnitTest {

  class FakeMovieDao : MovieDao {
    private val users = MutableStateFlow<List<UserAccount>>(emptyList())
    private val profiles = MutableStateFlow<List<Profile>>(emptyList())
    private val movies = MutableStateFlow<List<Movie>>(emptyList())
    private val watchlist = MutableStateFlow<List<Watchlist>>(emptyList())
    private val progress = MutableStateFlow<List<PlaybackProgress>>(emptyList())

    // --- USERS ---
    override suspend fun getUserById(userId: Int): UserAccount? = users.value.firstOrNull { it.id == userId }
    override suspend fun getUserByEmailDirect(email: String): UserAccount? = users.value.firstOrNull { it.email.equals(email, ignoreCase = true) }
    override suspend fun authenticateUser(email: String, password: String): UserAccount? = users.value.firstOrNull { it.email.equals(email, ignoreCase = true) && it.password == password }
    override suspend fun insertUser(user: UserAccount): Long {
      val newUser = if (user.id == 0) user.copy(id = users.value.size + 1) else user
      users.value = users.value.filter { it.id != newUser.id } + newUser
      return newUser.id.toLong()
    }
    override suspend fun updateUser(user: UserAccount) {
      users.value = users.value.filter { it.id != user.id } + user
    }
    override suspend fun deleteUser(user: UserAccount) {
      users.value = users.value.filter { it.id != user.id }
    }
    override suspend fun deleteUserById(userId: Int) {
      users.value = users.value.filter { it.id != userId }
    }
    override suspend fun setUserApproval(userId: Int, approved: Boolean) {
      users.value = users.value.map { if (it.id == userId) it.copy(isApproved = approved) else it }
    }
    override fun getAllUsers(): Flow<List<UserAccount>> = users
    override suspend fun getUserCount(): Int = users.value.size

    // --- PROFILES ---
    override fun getAllProfiles(): Flow<List<Profile>> = profiles
    override fun getProfilesForUser(userId: Int): Flow<List<Profile>> = flowOf(profiles.value.filter { it.userId == userId })
    override suspend fun deleteProfilesForUser(userId: Int) {
      profiles.value = profiles.value.filter { it.userId != userId }
    }
    override suspend fun getProfileById(id: Int): Profile? = profiles.value.firstOrNull { it.id == id }
    override suspend fun insertProfile(profile: Profile): Long {
      val newProfile = if (profile.id == 0) profile.copy(id = profiles.value.size + 1) else profile
      profiles.value = profiles.value + newProfile
      return newProfile.id.toLong()
    }
    override suspend fun deleteProfile(profile: Profile) {
      profiles.value = profiles.value.filter { it.id != profile.id }
    }

    // --- MOVIES ---
    override fun getAllMovies(): Flow<List<Movie>> = movies
    override fun getMovieById(id: Int): Flow<Movie?> = flowOf(movies.value.firstOrNull { it.id == id })
    override suspend fun getMovieByIdDirect(id: Int): Movie? = movies.value.firstOrNull { it.id == id }
    override suspend fun insertMovie(movie: Movie): Long {
      val newMovie = if (movie.id == 0) movie.copy(id = movies.value.size + 1) else movie
      movies.value = movies.value.filter { it.id != newMovie.id } + newMovie
      return newMovie.id.toLong()
    }
    override suspend fun updateMovie(movie: Movie) {
      movies.value = movies.value.filter { it.id != movie.id } + movie
    }
    override suspend fun deleteMovie(movie: Movie) {
      movies.value = movies.value.filter { it.id != movie.id }
    }

    // --- WATCHLIST ---
    override fun getWatchlistForProfile(profileId: Int): Flow<List<Movie>> {
      val ids = watchlist.value.filter { it.profileId == profileId }.map { it.movieId }
      return flowOf(movies.value.filter { it.id in ids })
    }
    override fun isInWatchlist(profileId: Int, movieId: Int): Flow<Boolean> {
      return flowOf(watchlist.value.any { it.profileId == profileId && it.movieId == movieId })
    }
    override suspend fun insertWatchlist(wl: Watchlist) {
      watchlist.value = watchlist.value + wl
    }
    override suspend fun deleteFromWatchlist(profileId: Int, movieId: Int) {
      watchlist.value = watchlist.value.filterNot { it.profileId == profileId && it.movieId == movieId }
    }
    override suspend fun deleteWatchlistForMovie(movieId: Int) {
      watchlist.value = watchlist.value.filterNot { it.movieId == movieId }
    }

    // --- PLAYBACK PROGRESS ---
    override fun getPlaybackProgress(profileId: Int, movieId: Int): Flow<PlaybackProgress?> {
      return flowOf(progress.value.firstOrNull { it.profileId == profileId && it.movieId == movieId })
    }
    override suspend fun getPlaybackProgressDirect(profileId: Int, movieId: Int): PlaybackProgress? {
      return progress.value.firstOrNull { it.profileId == profileId && it.movieId == movieId }
    }
    override fun getContinueWatchingMovies(profileId: Int): Flow<List<Movie>> {
      val ids = progress.value.filter { it.profileId == profileId }.map { it.movieId }
      return flowOf(movies.value.filter { it.id in ids })
    }
    override fun getPlaybackProgressForProfile(profileId: Int): Flow<List<PlaybackProgress>> {
      return flowOf(progress.value.filter { it.profileId == profileId })
    }
    override suspend fun insertPlaybackProgress(p: PlaybackProgress) {
      progress.value = progress.value.filterNot { it.profileId == p.profileId && it.movieId == p.movieId } + p
    }
    override suspend fun deletePlaybackProgress(profileId: Int, movieId: Int) {
      progress.value = progress.value.filterNot { it.profileId == profileId && it.movieId == movieId }
    }
    override suspend fun deletePlaybackProgressForMovie(movieId: Int) {
      progress.value = progress.value.filterNot { it.movieId == movieId }
    }
  }

  @Test
  fun testProfileSelectionAndDerivationPure() = runTest {
    val dao = FakeMovieDao()
    val repository = MovieRepository(dao)

    // Prepopulate database
    repository.prepopulateIfNeeded()

    // 1. Get populated profiles directly from repository
    val profilesList = repository.allProfiles.first()
    assertEquals(4, profilesList.size)

    val targetProfile = profilesList.first { !it.isKid }
    assertNotNull(targetProfile)

    // 2. Select profile
    val viewModel = MovieViewModel(repository)
    viewModel.selectProfile(targetProfile)

    // 3. Confirm currently selected profile matches
    assertEquals(targetProfile.id, viewModel.currentProfile.value?.id)
  }

  @Test
  fun testMetadataTitleExtractor() {
    val service = com.example.data.repository.MetadataService()
    assertEquals("Interstellar", service.extractTitle("Interstellar.2014.1080p.BluRay.x264.mp4"))
    assertEquals("Stranger Things", service.extractTitle("https://drive.google.com/file/d/123/Stranger.Things.S01E01.mp4"))
  }
}
