package org.jetbrains.bazel.sync.workspace.languages.jvm

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus

/**
 * Stores the per-module javac options computed by the Bazel sync, so that the IDE Java compiler uses them.
 * Without an implementation the options are dropped. The Language Server has none: it never runs a javac build.
 */
@ApiStatus.Internal
interface JavacOptionsUpdater {
  /**
   * @param optionsByModuleName javac options joined by spaces, keyed by module name
   */
  fun updateJavacOptions(project: Project, optionsByModuleName: Map<String, String>)

  companion object {
    val ep: ExtensionPointName<JavacOptionsUpdater> =
      ExtensionPointName.create("org.jetbrains.bazel.javacOptionsUpdater")
  }
}
