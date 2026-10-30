package org.jetbrains.bazel.run.synthetic

import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.runConfigurationType
import com.intellij.lang.Language
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import org.jetbrains.bazel.run.config.BazelRunConfigurationType
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Test

@BazelTestApplication
class GenerateSyntheticTargetRunTaskProviderTest {
  private val myFixture by bazelCodeInsightFixture()

  @Test
  fun `test provider creates task`() {
    val project = myFixture.project
    val configurationType = runConfigurationType<BazelRunConfigurationType>()
    val factory = configurationType.configurationFactories.first()
    val settings = RunManager.getInstance(project).createConfiguration("test", factory)

    val provider = BeforeRunTaskProvider.getProvider(project, GENERATE_SYNTHETIC_PROVIDER_ID)
    provider.shouldNotBeNull()

    val task = provider.createTask(settings.configuration)
    task.shouldNotBeNull()

    val taskState = task.taskState
    taskState.shouldNotBeNull()
    taskState.target.shouldBeEmpty()
    taskState.params.shouldBeEmpty()
    taskState.language.shouldBe(Language.ANY.id)
  }
}
