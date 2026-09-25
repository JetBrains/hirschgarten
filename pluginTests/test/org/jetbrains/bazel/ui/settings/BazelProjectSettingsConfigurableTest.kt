package org.jetbrains.bazel.ui.settings

import com.intellij.openapi.application.UI
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.replaceService
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.sync.BazelEnvironmentService
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.`when`
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class BazelProjectSettingsConfigurableTest : MockProjectBaseTest() {
  private lateinit var detectedBuildifier: Path
  private lateinit var environmentService: BazelEnvironmentService

  @BeforeEach
  fun setUp(): Unit = timeoutRunBlocking {
    detectedBuildifier = createExecutable("detected-buildifier")
    environmentService = mock(BazelEnvironmentService::class.java)
    `when`(environmentService.findInPath("buildifier")).thenAnswer { detectedBuildifier }
    project.replaceService(BazelEnvironmentService::class.java, environmentService, disposable)
  }

  @AfterEach
  fun tearDown(): Unit = timeoutRunBlocking {
    withContext(Dispatchers.UI) {
      reset(environmentService)
    }
    reset(environmentService)
  }

  @Test
  fun `revert clears the override and follows detection changes`(): Unit = timeoutRunBlocking {
    val customBuildifier = createExecutable("custom-buildifier")
    project.bazelProjectSettings = project.bazelProjectSettings.withNewBuildifierExecutablePath(customBuildifier)

    val (text, modified) = withConfigurable { configurable, field ->
      val textField = field.textField as ExtendableTextField
      textField.extensions.single {
        it.tooltip == BazelPluginBundle.message("project.settings.buildifier.revert.tooltip")
      }.actionOnClick.run()
      val modified = configurable.isModified()
      configurable.apply()
      field.text to modified
    }

    assertEquals("", text)
    assertTrue(modified)
    assertNull(project.bazelProjectSettings.buildifierExecutablePath)
    detectedBuildifier = createExecutable("new-detected-buildifier")
    assertEquals(detectedBuildifier.toString(), project.bazelProjectSettings.getBuildifierPathString(project))
  }

  @Test
  fun `an explicit detected path stays visible`(): Unit = timeoutRunBlocking {
    project.bazelProjectSettings = project.bazelProjectSettings.withNewBuildifierExecutablePath(detectedBuildifier)

    val (text, modified) = withConfigurable { configurable, field ->
      field.text to configurable.isModified()
    }

    assertEquals(detectedBuildifier.toString(), text)
    assertFalse(modified)
  }

  @Test
  fun `reset restores the saved override after an edit`(): Unit = timeoutRunBlocking {
    val customBuildifier = createExecutable("custom-buildifier")
    project.bazelProjectSettings = project.bazelProjectSettings.withNewBuildifierExecutablePath(customBuildifier)

    val (text, modified) = withConfigurable { configurable, field ->
      field.text = ""
      configurable.reset()
      field.text to configurable.isModified()
    }

    assertEquals(customBuildifier.toString(), text)
    assertFalse(modified)
    assertEquals(customBuildifier, project.bazelProjectSettings.buildifierExecutablePath)
  }

  private fun createExecutable(name: String): Path =
    projectDir.get().resolve(name).createFile().apply {
      check(toFile().setExecutable(true))
    }

  private suspend fun <T> withConfigurable(action: (BazelProjectSettingsConfigurable, TextFieldWithBrowseButton) -> T): T =
    withContext(Dispatchers.UI) {
      val configurable = BazelProjectSettingsConfigurable(project)
      try {
        val panel = configurable.createComponent()
        configurable.reset()
        val field = checkNotNull(UIUtil.findComponentOfType(panel, TextFieldWithBrowseButton::class.java))
        action(configurable, field)
      }
      finally {
        configurable.disposeUIResources()
      }
    }
}
