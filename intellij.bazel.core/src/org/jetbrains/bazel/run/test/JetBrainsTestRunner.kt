package org.jetbrains.bazel.run.test

import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.bazel.languages.projectview.projectView
import org.jetbrains.bazel.languages.projectview.useJetBrainsTestRunner
import org.jetbrains.bazel.run.BazelRunConfigurationState
import org.jetbrains.bazel.run.state.HasEnv
import org.jetbrains.bazel.run.state.HasTestFilter
import org.jetbrains.bazel.runnerAction.BazelRunnerActionDescriptor

// Constants copied from JUnit5BazelRunner
private const val JB_TEST_UNIQUE_IDS = "JB_TEST_UNIQUE_IDS"
private const val JB_TEST_FILTER = "JB_TEST_FILTER"
private const val JB_IDE_SM_RUN = "JB_IDE_SM_RUN"

@VisibleForTesting
@ApiStatus.Internal
var forceDisableJetBrainsTestRunner = false

@ApiStatus.Internal
fun Project.useJetBrainsTestRunner(): Boolean {
  if (forceDisableJetBrainsTestRunner) {
    return false
  }
  return projectView().useJetBrainsTestRunner
}

@ApiStatus.Internal
fun createTestFilterDescriptor(project: Project, testFilter: String): BazelRunnerActionDescriptor =
  if (project.useJetBrainsTestRunner()) {
    BazelRunnerActionDescriptor(
      testFilter = null,
      env = mapOf(
        JB_TEST_FILTER to testFilter,
        JB_IDE_SM_RUN to "true",
      ),
    )
  }
  else {
    BazelRunnerActionDescriptor(
      testFilter = testFilter,
    )
  }

internal fun setTestUniqueIds(state: BazelRunConfigurationState<*>, testUniqueIds: List<String>) {
  (state as? HasTestFilter)?.testFilter = null
  (state as? HasEnv)?.env?.envs?.let {
    it.remove(JB_TEST_FILTER)
    it[JB_TEST_UNIQUE_IDS] = testUniqueIds.joinToString(separator = ";")
    it[JB_IDE_SM_RUN] = "true"
  }
}

internal fun getTestUniqueIds(state: BazelRunConfigurationState<*>): List<String>? {
  (state as? HasEnv)?.env?.envs?.let {
    return it[JB_TEST_UNIQUE_IDS]?.split(";")
  }
  return null
}
