package org.jetbrains.bazel.sdkcompat

import com.intellij.jarRepository.DisableCompilationDependenciesResolutionTask
import com.intellij.openapi.project.Project

class DisableCompilationDependenciesResolutionImpl : DisableCompilationDependenciesResolutionTask {
  override fun shouldDisable(project: Project): Boolean = true
}
