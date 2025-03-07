package org.jetbrains.bazel.sdkcompat

import com.intellij.openapi.project.Project

/**
 * This is just a placeholder as the interface [com.intellij.jarRepository.DisableCompilationDependenciesResolutionTask] is not available in 243.
 * For more information, check out the impl in v251.
 */
abstract class DisableCompilationDependenciesResolutionCompat {
  abstract fun shouldDisableCompat(project: Project): Boolean
}
