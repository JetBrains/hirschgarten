package org.jetbrains.bazel.sdkcompat

import com.intellij.jarRepository.DisableCompilationDependenciesResolutionTask
import com.intellij.openapi.project.Project

abstract class DisableCompilationDependenciesResolutionCompat : DisableCompilationDependenciesResolutionTask {
  abstract fun shouldDisableCompat(project: Project): Boolean

  override fun shouldDisable(project: Project): Boolean = shouldDisableCompat(project)
}
