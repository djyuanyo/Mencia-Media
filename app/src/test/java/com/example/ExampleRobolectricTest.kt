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
}
