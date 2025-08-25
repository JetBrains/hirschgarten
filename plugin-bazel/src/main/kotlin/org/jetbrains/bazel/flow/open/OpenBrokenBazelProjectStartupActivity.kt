package org.jetbrains.bazel.flow.open

import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.jetbrains.bazel.config.BazelProjectProperties
import org.jetbrains.bazel.config.rootDir

// See https://youtrack.jetbrains.com/issue/BAZEL-1500
private class OpenBrokenBazelProjectStartupActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (!project.serviceAsync<BazelProjectProperties>().isBrokenBazelProject) {
      return
    }
    performOpenBazelProject(project, project.rootDir)
  }
}
