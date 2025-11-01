package org.jetbrains.bazel.run.synthetic

import com.intellij.execution.RunManager
import com.intellij.execution.configurations.runConfigurationType
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.nulls.shouldNotBeNull
import org.jetbrains.bazel.run.config.BazelRunConfigurationType

class GenerateSyntheticTargetRunTaskProviderTest : BasePlatformTestCase() {

  fun `test provider creates task`() {
    val provider = GenerateSyntheticTargetRunTaskProvider()
    val configurationType = runConfigurationType<BazelRunConfigurationType>()
    val factory = configurationType.configurationFactories.first()
    val settings = RunManager.getInstance(project).createConfiguration("test", factory)

    val task = provider.createTask(settings.configuration)

    task.shouldNotBeNull()
    task.taskState.shouldNotBeNull()
  }
}
