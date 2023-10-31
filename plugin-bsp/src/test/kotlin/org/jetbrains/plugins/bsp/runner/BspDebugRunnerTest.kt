package org.jetbrains.plugins.bsp.runner

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import io.kotest.matchers.shouldBe
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.flow.open.BuildToolId
import org.jetbrains.plugins.bsp.ui.configuration.run.BspDebugType
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunConfiguration
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunConfigurationType
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunFactory
import org.jetbrains.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.Test
import javax.swing.Icon

class BspDebugRunnerTest : MockProjectBaseTest() {
  @Test
  fun `should refuse to run non-debug executor`() {
    val executorId = WRONG_EXECUTOR
    val runProfile = createBspRunConfiguration(BspDebugType.JDWP)

    BspDebugRunner().canRun(executorId, runProfile) shouldBe false
  }

  @Test
  fun `should refuse to run other types of run configurations`() {
    val executorId = CORRECT_EXECUTOR
    val runProfile = MockRunConfiguration()

    BspDebugRunner().canRun(executorId, runProfile) shouldBe false
  }

  @Test
  fun `should refuse to run targets with no compatible debugging type`() {
    val executorId = CORRECT_EXECUTOR
    val runProfile = createBspRunConfiguration(null)

    BspDebugRunner().canRun(executorId, runProfile) shouldBe false
  }

  @Test
  fun `should agree to run valid debug requests`() {
    val executorId = CORRECT_EXECUTOR
    val runProfile = createBspRunConfiguration(BspDebugType.JDWP)

    BspDebugRunner().canRun(executorId, runProfile) shouldBe true
  }

  private fun createBspRunConfiguration(debugType: BspDebugType?): BspRunConfiguration {
    val type = BspRunConfigurationType(project.apply { buildToolId = BuildToolId("test") })
    val factory = BspRunFactory(type)
    val template = factory.createTemplateConfiguration(project) as BspRunConfiguration
    template.apply {
      this.debugType = debugType
    }
    return template
  }
}

private const val CORRECT_EXECUTOR = DefaultDebugExecutor.EXECUTOR_ID
private const val WRONG_EXECUTOR = DefaultRunExecutor.EXECUTOR_ID

private class MockRunConfiguration : RunConfiguration {
  override fun getState(p0: Executor, p1: ExecutionEnvironment): RunProfileState? = null
  override fun getName(): String = "MockRunConfiguration"
  override fun getIcon(): Icon? = null
  override fun clone(): RunConfiguration = this
  override fun getFactory(): ConfigurationFactory? = null
  override fun setName(p0: String?) {}
  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> = error("")
  override fun getProject(): Project = error("")
}
