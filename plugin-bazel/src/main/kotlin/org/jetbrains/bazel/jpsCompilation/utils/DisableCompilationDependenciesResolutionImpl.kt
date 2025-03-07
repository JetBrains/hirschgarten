package org.jetbrains.bazel.jpsCompilation.utils

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.sdkcompat.DisableCompilationDependenciesResolutionCompat

/**
 * [com.intellij.jarRepository.CompilationDependenciesResolutionTask] is used for resolving Maven repositories before JPS build.
 * This task is useless for JPS over Bazel because all the dependencies are resolved by Bazel.
 * Not only that, due to the current impl, this task is a performance issue with a large time overhead when using JPS over Bazel.
 * A proper fix will be introduced in `2025.1.1`.
 * For now, we need to implement this interface to explicitly skip this task.
 * [Relevant ticket](https://youtrack.jetbrains.com/issue/IDEA-367562)
 */
class DisableCompilationDependenciesResolutionImpl : DisableCompilationDependenciesResolutionCompat() {
  override fun shouldDisableCompat(project: Project): Boolean = project.isBazelProject
}
