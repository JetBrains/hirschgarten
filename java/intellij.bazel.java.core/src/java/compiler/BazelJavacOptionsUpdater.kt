package org.jetbrains.bazel.java.compiler

import com.intellij.compiler.impl.javaCompiler.javac.JavacConfiguration
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync.workspace.languages.jvm.JavacOptionsUpdater

internal class BazelJavacOptionsUpdater : JavacOptionsUpdater {
  override fun updateJavacOptions(project: Project, optionsByModuleName: Map<String, String>) {
    JavacConfiguration.getOptions(project, JavacConfiguration::class.java).ADDITIONAL_OPTIONS_OVERRIDE =
      HashMap(optionsByModuleName) // the settings UI mutates this map in place
  }
}
