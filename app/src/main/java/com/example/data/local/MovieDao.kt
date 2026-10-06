package com.example.data.local

import androidx.room.*
import com.example.data.model.Movie
import com.example.data.model.PlaybackProgress
import com.example.data.model.Profile
import com.example.data.model.UserAccount
import com.example.data.model.Watchlist
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {

    // --- USER ACCOUNTS ---
    @Query("SELECT * FROM user_accounts WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmailDirect(email: String): UserAccount?

    @Query("SELECT * FROM user_accounts WHERE LOWER(email) = LOWER(:email) AND password = :password LIMIT 1")
    suspend fun authenticateUser(email: String, password: String): UserAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccount): Long

    @Query("SELECT * FROM user_accounts ORDER BY createdAt ASC")
    fun getAllUsers(): Flow<List<UserAccount>>

    @Query("SELECT COUNT(*) FROM user_accounts")
    suspend fun getUserCount(): Int

    // --- PROFILES ---
    @Query("SELECT * FROM profiles ORDER BY createdAt ASC")
    fun getAllProfiles(): Flow<List<Profile>>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Int): Profile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile): Long

    @Delete
    suspend fun deleteProfile(profile: Profile)

    // --- MOVIES ---
    @Query("SELECT * FROM movies ORDER BY addedAt DESC")
    fun getAllMovies(): Flow<List<Movie>>

    @Query("SELECT * FROM movies WHERE id = :id LIMIT 1")
    fun getMovieById(id: Int): Flow<Movie?>

    @Query("SELECT * FROM movies WHERE id = :id LIMIT 1")
    suspend fun getMovieByIdDirect(id: Int): Movie?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovie(movie: Movie): Long

    @Update
    suspend fun updateMovie(movie: Movie)

    @Delete
    suspend fun deleteMovie(movie: Movie)

    // --- WATCHLIST ---
    @Query("""
        SELECT m.* FROM movies m 
        INNER JOIN watchlist w ON m.id = w.movieId 
        WHERE w.profileId = :profileId 
        ORDER BY w.addedAt DESC
    """)
    fun getWatchlistForProfile(profileId: Int): Flow<List<Movie>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE profileId = :profileId AND movieId = :movieId)")
    fun isInWatchlist(profileId: Int, movieId: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlist(watchlist: Watchlist)

    @Query("DELETE FROM watchlist WHERE profileId = :profileId AND movieId = :movieId")
    suspend fun deleteFromWatchlist(profileId: Int, movieId: Int)

    // --- PLAYBACK PROGRESS ---
    @Query("SELECT * FROM playback_progress WHERE profileId = :profileId AND movieId = :movieId LIMIT 1")
    fun getPlaybackProgress(profileId: Int, movieId: Int): Flow<PlaybackProgress?>

    @Query("SELECT * FROM playback_progress WHERE profileId = :profileId AND movieId = :movieId LIMIT 1")
    suspend fun getPlaybackProgressDirect(profileId: Int, movieId: Int): PlaybackProgress?

    @Query("""
        SELECT m.* FROM movies m 
        INNER JOIN playback_progress p ON m.id = p.movieId 
        WHERE p.profileId = :profileId 
        ORDER BY p.lastAccessed DESC
    """)
    fun getContinueWatchingMovies(profileId: Int): Flow<List<Movie>>

    @Query("SELECT * FROM playback_progress WHERE profileId = :profileId ORDER BY lastAccessed DESC")
    fun getPlaybackProgressForProfile(profileId: Int): Flow<List<PlaybackProgress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaybackProgress(progress: PlaybackProgress)

    @Query("DELETE FROM playback_progress WHERE profileId = :profileId AND movieId = :movieId")
    suspend fun deletePlaybackProgress(profileId: Int, movieId: Int)
}
