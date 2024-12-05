package org.jetbrains.bazel.flow.open

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.jetbrains.plugins.bsp.config.isBrokenBspProject
import org.jetbrains.plugins.bsp.config.rootDir

// See https://youtrack.jetbrains.com/issue/BAZEL-1500
class OpenBrokenBazelProjectStartupActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (!project.isBrokenBspProject) return
    performOpenBazelProjectViaBspPlugin(project, project.rootDir)
  }
}
