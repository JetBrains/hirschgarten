package org.jetbrains.bazel.run.test

import com.intellij.openapi.project.Project
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.languages.projectview.ProjectViewService
import org.jetbrains.bazel.languages.projectview.useJetBrainsTestRunner
import org.jetbrains.bazel.run.BazelRunConfigurationState
import org.jetbrains.bazel.run.state.HasEnv
import org.jetbrains.bazel.run.state.HasTestFilter

// Constants copied from JUnit5BazelRunner
private const val JB_TEST_UNIQUE_IDS = "JB_TEST_UNIQUE_IDS"
private const val JB_TEST_FILTER = "JB_TEST_FILTER"
private const val JB_IDE_SM_RUN = "JB_IDE_SM_RUN"

@VisibleForTesting
public var forceDisableJetBrainsTestRunner = false

fun Project.useJetBrainsTestRunner(): Boolean {
  if (forceDisableJetBrainsTestRunner) {
    return false
  }
  if (!isBazelProject) return false
  return ProjectViewService.getInstance(this).getCachedProjectView().useJetBrainsTestRunner
}

fun setTestFilter(project: Project, state: BazelRunConfigurationState<*>, testFilter: String?) {
  if (project.useJetBrainsTestRunner()) {
    (state as? HasTestFilter)?.testFilter = null
    (state as? HasEnv)?.env?.envs?.let {
      it.remove(JB_TEST_UNIQUE_IDS)
      if (testFilter != null) {
        it[JB_TEST_FILTER] = testFilter
      } else {
        it.remove(JB_TEST_FILTER)
      }
      it[JB_IDE_SM_RUN] = "true"
    }
  } else {
    (state as? HasTestFilter)?.testFilter = testFilter
    (state as? HasEnv)?.env?.envs?.let {
      it.remove(JB_TEST_UNIQUE_IDS)
      it.remove(JB_TEST_FILTER)
      it.remove(JB_IDE_SM_RUN)
    }
  }
}

fun setTestUniqueIds(state: BazelRunConfigurationState<*>, testUniqueIds: List<String>) {
  (state as? HasTestFilter)?.testFilter = null
  (state as? HasEnv)?.env?.envs?.let {
    it.remove(JB_TEST_FILTER)
    it[JB_TEST_UNIQUE_IDS] = testUniqueIds.joinToString(separator = ";")
    it[JB_IDE_SM_RUN] = "true"
  }
}

fun getTestUniqueIds(state: BazelRunConfigurationState<*>): List<String>? {
  (state as? HasEnv)?.env?.envs?.let {
    return it[JB_TEST_UNIQUE_IDS]?.split(";")
  }
  return null
}
