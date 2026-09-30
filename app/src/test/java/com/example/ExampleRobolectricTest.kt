package com.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.example.ai.capability.AIProviderType
import com.example.ui.MainViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    assertEquals("Summer Winter", appName)
  }

  @Test
  fun `create MainViewModel via AndroidViewModelFactory`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
    val viewModel = factory.create(MainViewModel::class.java)
    assertNotNull(viewModel)
    assertEquals("Summer", viewModel.getPersonality().shortName)
    assertNotNull(viewModel.aiDiagnostics.value)
    assertEquals(AIProviderType.DETERMINISTIC_LOCAL, viewModel.aiDiagnostics.value.currentProvider)
    assertTrue(viewModel.aiDiagnostics.value.isFallbackActive)
  }

  @Test
  fun `MainViewModel delegates query to SummerOrchestrator`() = runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
    val viewModel = factory.create(MainViewModel::class.java)

    // Direct orchestrator execution via ViewModel
    val interaction = viewModel.orchestrator.handleUserInput("Hello Summer")
    assertEquals("Hello. I'm Summer. How can I assist you?", interaction.response?.text)
    assertNotNull(viewModel.recentResponse.value)
  }

  @Test
  fun `MainViewModel refreshes AI capabilities without crashing`() = runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
    val viewModel = factory.create(MainViewModel::class.java)

    viewModel.refreshAICapabilities()
    val diag = viewModel.aiDiagnostics.value
    assertNotNull(diag)
    assertTrue(diag.deviceApiLevel > 0)
  }
}
