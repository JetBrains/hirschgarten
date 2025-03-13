package org.jetbrains.bazel.startup

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.jetbrains.bazel.config.isBazelProject

abstract class BazelProjectActivity : ProjectActivity {
  final override suspend fun execute(project: Project) {
    if (project.isBazelProject) {
      project.executeForBazelProject()
    }
  }

  abstract suspend fun Project.executeForBazelProject()
}
