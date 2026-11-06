package org.jetbrains.bazel.clion

import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.clion.run.CcRunHandler
import org.jetbrains.bazel.clion.run.debug.CcDebugRunner
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.ccProject
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.config.bazelRunConfigurationFactory
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val CC_RUN_HANDLER_PROVIDER_ID = "CcRunHandlerProvider"

@CcTestApplication
class CcForcedRunHandlerTest {
  private val directoryFixture = tempPathFixture()
  private val directory by directoryFixture
  private val project by projectFixture(pathFixture = directoryFixture)

  private val snapshot = ccProject {
    plainTarget {
      label("//go:main")
      kind("go_binary", RuleType.BINARY)
    }
    plainTarget {
      label("//go:other")
      kind("go_binary", RuleType.BINARY)
    }
  }

  @BeforeEach
  fun setUp(): Unit = timeoutRunBlocking {
    initializeBazelProject(project, directory)
    project.service<WorkspaceSnapshotService>().update { snapshot to Unit }
  }

  @Test
  fun testChooserListsCcHandler() {
    val ids = RunHandlerProvider.findSelectableRunHandlerProviders().map { it.id }
    assertThat(ids).contains(CC_RUN_HANDLER_PROVIDER_ID)
  }

  @Test
  fun testDetectedHandlerIsNotCc(): Unit = timeoutRunBlocking {
    val configuration = createConfiguration("//go:main")
    assertThat(configuration.forcedHandlerProviderId).isNull()
    assertThat(configuration.handler).isNotInstanceOf(CcRunHandler::class.java)
  }

  @Test
  fun testForcedCcHandlerDebugsNonCcTarget(): Unit = timeoutRunBlocking {
    val configuration = createConfiguration("//go:main")
    withContext(Dispatchers.EDT) { configuration.updateForcedHandlerProvider(CC_RUN_HANDLER_PROVIDER_ID) }

    assertThat(configuration.handler).isInstanceOf(CcRunHandler::class.java)
    assertThat(CcDebugRunner().canRun(DefaultDebugExecutor.EXECUTOR_ID, configuration)).isTrue()
    configuration.checkConfigurationUnsafe()
  }

  @Test
  fun testForcedHandlerSurvivesTargetChange(): Unit = timeoutRunBlocking {
    val configuration = createConfiguration("//go:main")
    withContext(Dispatchers.EDT) {
      configuration.updateForcedHandlerProvider(CC_RUN_HANDLER_PROVIDER_ID)
      configuration.updateTargets(listOf(Label.parse("//go:other")))
    }

    assertThat(configuration.handler).isInstanceOf(CcRunHandler::class.java)
  }

  @Test
  fun testForcedHandlerIsCloned(): Unit = timeoutRunBlocking {
    val configuration = createConfiguration("//go:main")
    val clone = withContext(Dispatchers.EDT) {
      configuration.updateForcedHandlerProvider(CC_RUN_HANDLER_PROVIDER_ID)
      configuration.clone() as BazelRunConfiguration
    }

    assertThat(clone.forcedHandlerProviderId).isEqualTo(CC_RUN_HANDLER_PROVIDER_ID)
    assertThat(clone.handler).isInstanceOf(CcRunHandler::class.java)
  }

  @Test
  fun testResetToDetectedHandler(): Unit = timeoutRunBlocking {
    val configuration = createConfiguration("//go:main")
    withContext(Dispatchers.EDT) {
      configuration.updateForcedHandlerProvider(CC_RUN_HANDLER_PROVIDER_ID)
      configuration.updateForcedHandlerProvider(null)
    }

    assertThat(configuration.forcedHandlerProviderId).isNull()
    assertThat(configuration.handler).isNotInstanceOf(CcRunHandler::class.java)
  }

  private suspend fun createConfiguration(vararg labels: String): BazelRunConfiguration = withContext(Dispatchers.EDT) {
    val settings = RunManager.getInstance(project).createConfiguration("test", bazelRunConfigurationFactory)
    val configuration = settings.configuration as BazelRunConfiguration
    configuration.updateTargets(labels.map { Label.parse(it) })
    configuration
  }
}
