package org.jetbrains.plugins.bsp.startup

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.jetbrains.plugins.bsp.config.isBspProject

abstract class BspProjectActivity : ProjectActivity {
  final override suspend fun execute(project: Project) {
    if (project.isBspProject) {
      project.executeForBspProject()
    }
  }

  abstract suspend fun Project.executeForBspProject()
}
