package org.jetbrains.bazel.startup.utils

import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.jetbrains.bazel.config.BazelProjectProperties

// todo to be removed
abstract class BazelProjectActivity : ProjectActivity {
  final override suspend fun execute(project: Project) {
    if (project.serviceAsync<BazelProjectProperties>().isBazelProject) {
      executeForBazelProject(project)
    }
  }

  abstract suspend fun executeForBazelProject(project: Project)
}
