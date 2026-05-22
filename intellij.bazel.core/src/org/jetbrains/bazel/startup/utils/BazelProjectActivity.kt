package org.jetbrains.bazel.startup.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.isBazelProject

// todo to be removed
@ApiStatus.Internal
abstract class BazelProjectActivity : ProjectActivity {
  final override suspend fun execute(project: Project) {
    if (project.isBazelProject) {
      executeForBazelProject(project)
    }
  }

  abstract suspend fun executeForBazelProject(project: Project)
}
