package org.jetbrains.bazel.ui.settings

import com.intellij.openapi.application.UI
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.replaceService
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.util.ui.UIUtil
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.sync.BazelEnvironmentService
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.testproject.testBazelProjectFixture
import org.jetbrains.bazel.test.framework.testproject.workspaceRoot
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.`when`
import java.nio.file.Path
import kotlin.io.path.createFile

@BazelTestApplication
internal class BazelProjectSettingsConfigurableTest {
  private val testBazel by testBazelProjectFixture(openAfterCreation = false)
  private val project get() = testBazel.project

  private lateinit var detectedBuildifier: Path
  private lateinit var environmentService: BazelEnvironmentService

  @BeforeEach
  fun setUp(): Unit = timeoutRunBlocking {
    detectedBuildifier = createExecutable("detected-buildifier")
    environmentService = mock(BazelEnvironmentService::class.java)
    `when`(environmentService.findInPath("buildifier")).thenAnswer { detectedBuildifier }
    project.replaceService(BazelEnvironmentService::class.java, environmentService, testBazel.disposable)
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

    text shouldBe ""
    modified shouldBe true
    project.bazelProjectSettings.buildifierExecutablePath.shouldBeNull()
    detectedBuildifier = createExecutable("new-detected-buildifier")
    project.bazelProjectSettings.getBuildifierPathString(project) shouldBe detectedBuildifier.toString()
  }

  @Test
  fun `an explicit detected path stays visible`(): Unit = timeoutRunBlocking {
    project.bazelProjectSettings = project.bazelProjectSettings.withNewBuildifierExecutablePath(detectedBuildifier)

    val (text, modified) = withConfigurable { configurable, field ->
      field.text to configurable.isModified()
    }

    text shouldBe detectedBuildifier.toString()
    modified shouldBe false
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

    text shouldBe customBuildifier.toString()
    modified shouldBe false
    project.bazelProjectSettings.buildifierExecutablePath shouldBe customBuildifier
  }

  private fun createExecutable(name: String): Path =
    testBazel.workspaceRoot.resolve(name).createFile().apply {
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
