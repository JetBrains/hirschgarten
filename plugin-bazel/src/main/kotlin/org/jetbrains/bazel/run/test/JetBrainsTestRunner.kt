package org.jetbrains.bazel.run.test

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.languages.projectview.ProjectViewService
import org.jetbrains.bazel.languages.projectview.useJetBrainsTestRunner
import org.jetbrains.bazel.run.BazelRunConfigurationState
import org.jetbrains.bazel.run.state.HasEnv
import org.jetbrains.bazel.run.state.HasTestFilter

public const val JB_TEST_UNIQUE_IDS = "JB_TEST_UNIQUE_IDS"
public const val JB_TEST_FILTER = "JB_TEST_FILTER"

fun Project.useJetBrainsTestRunner(): Boolean =
  ProjectViewService.getInstance(this).getCachedProjectView().useJetBrainsTestRunner

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
    }
  } else {
    (state as? HasTestFilter)?.testFilter = testFilter
    (state as? HasEnv)?.env?.envs?.let {
      it.remove(JB_TEST_UNIQUE_IDS)
      it.remove(JB_TEST_FILTER)
    }
  }
}

fun setTestUniqueIds(state: BazelRunConfigurationState<*>, testUniqueIds: String) {
  (state as? HasTestFilter)?.testFilter = null
  (state as? HasEnv)?.env?.envs?.let {
    it.remove(JB_TEST_FILTER)
    it[JB_TEST_UNIQUE_IDS] = testUniqueIds
  }
}
