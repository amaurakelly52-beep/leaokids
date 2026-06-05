package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.LeaoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Leão Kids", appName)
  }

  @Test
  fun testVideoRecommendationsAndBlacklist() = runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = LeaoViewModel(app)
    
    // Select one of our preset videos
    val presetVideos = viewModel.presetVideos
    val firstVideo = presetVideos.first()
    
    viewModel.selectVideoAndNavigate(firstVideo)
    
    // Wait for the initial recommendations combine flow
    delay(500)
    
    val initialRecs = viewModel.recommendedVideos.value
    assertTrue("Recommendations list should not be empty", initialRecs.isNotEmpty())
    
    // Identify a channel in the recommendations to block
    val targetVideo = initialRecs.first()
    val channelToBlock = targetVideo.channelName
    
    viewModel.addBlockedChannel(channelToBlock)
    delay(500)
    
    val recsAfterChannelBlock = viewModel.recommendedVideos.value
    assertFalse(
      "Blacklisted channel '$channelToBlock''s videos should not appear in recommendations",
      recsAfterChannelBlock.any { it.channelName.contains(channelToBlock, ignoreCase = true) }
    )
    
    // Identify a keyword from one of the remaining videos to block
    if (recsAfterChannelBlock.isNotEmpty()) {
      val wordVideo = recsAfterChannelBlock.first()
      val titleWord = wordVideo.title.split(" ").firstOrNull() ?: "Foguete"
      
      viewModel.addBlockedWord(titleWord)
      delay(500)
      
      val finalRecs = viewModel.recommendedVideos.value
      assertFalse(
        "Blacklisted word '$titleWord''s videos should not appear in recommendations",
        finalRecs.any { it.title.contains(titleWord, ignoreCase = true) }
      )
    }
  }
}

