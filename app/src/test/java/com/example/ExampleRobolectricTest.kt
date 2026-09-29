package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.MovieRepository
import com.example.data.model.Profile
import com.example.ui.viewmodel.MovieViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var database: AppDatabase
  private lateinit var repository: MovieRepository
  private lateinit var viewModel: MovieViewModel

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = MovieRepository(database.movieDao())
    viewModel = MovieViewModel(repository)
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun testProfileSelectionAndStateDerivation() = runTest {
    // 1. Initial configuration check: Database population
    repository.prepopulateIfNeeded()
    
    val profilesList = repository.allProfiles.first()
    assertEquals(4, profilesList.size)
    
    val targetProfile = profilesList.first { !it.isKid }
    assertNotNull(targetProfile)

    // 2. Select profile
    viewModel.selectProfile(targetProfile)
    
    // 3. Confirm currently selected profile matches
    assertEquals(targetProfile.id, viewModel.currentProfile.value?.id)
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertNotNull(appName)
  }

  @Test
  fun testTheTVDBEpisodeSerialization() {
    val episodes = listOf(
      com.example.data.model.EpisodeData(seasonNumber = 1, episodeNumber = 1, title = "Piloto", overview = "Inicio de la serie", runtime = "50 min"),
      com.example.data.model.EpisodeData(seasonNumber = 1, episodeNumber = 2, title = "Capítulo 2", overview = "Desarrollo", runtime = "48 min")
    )
    val json = com.example.data.model.EpisodeData.listToJson(episodes)
    val restored = com.example.data.model.EpisodeData.listFromJson(json)
    assertEquals(2, restored.size)
    assertEquals("Piloto", restored[0].title)
    assertEquals(1, restored[0].episodeNumber)
    assertEquals("Capítulo 2", restored[1].title)
  }

  @Test
  fun testAddMovieAndSeriesWithoutVideoUrlAppearsInCatalog() = runTest {
    // 1. Initial database population
    repository.prepopulateIfNeeded()
    val initialCount = repository.allMovies.first().size

    // 2. Add movie without video link (videoUrl is empty)
    viewModel.addMovie(
      title = "Dune: Parte Dos",
      description = "Paul Atreides se une a Chani y a los Fremen.",
      videoUrl = "",
      posterUrl = "https://image.tmdb.org/t/p/w500/dune.jpg",
      category = "Películas",
      genre = "Sci-Fi",
      year = "2024",
      duration = "166 min",
      isFeatured = false,
      cast = "Timothée Chalamet, Zendaya",
      imdbRating = "8.6",
      imdbId = "tt15239678"
    )

    // 3. Add series without video link
    viewModel.addMovie(
      title = "Breaking Bad",
      description = "Un profesor de química diagnosticado con cáncer.",
      videoUrl = "",
      posterUrl = "https://image.tmdb.org/t/p/w500/bb.jpg",
      category = "Series",
      genre = "Drama",
      year = "2008",
      duration = "5 temporadas",
      isFeatured = false,
      cast = "Bryan Cranston, Aaron Paul",
      imdbRating = "9.5",
      imdbId = "tt0903747"
    )

    // 4. Verify catalog now contains both new items even without videoUrl
    val updatedMovies = repository.allMovies.first()
    assertEquals(initialCount + 2, updatedMovies.size)

    val dune = updatedMovies.firstOrNull { it.title == "Dune: Parte Dos" }
    assertNotNull(dune)
    assertEquals("", dune?.videoUrl)
    assertEquals("8.6", dune?.imdbRating)

    val bb = updatedMovies.firstOrNull { it.title == "Breaking Bad" }
    assertNotNull(bb)
    assertEquals("Series", bb?.category)
    assertEquals("", bb?.videoUrl)
  }
}
