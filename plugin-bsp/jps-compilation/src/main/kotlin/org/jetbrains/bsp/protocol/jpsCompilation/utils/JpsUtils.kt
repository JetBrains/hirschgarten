package org.jetbrains.bsp.protocol.jpsCompilation.utils

import com.intellij.openapi.compiler.CompilerManager
import com.intellij.openapi.project.Project

object JpsUtils {
  fun prepareForJps(project: Project) {
    val compilerManager = CompilerManager.getInstance(project)
    compilerManager.beforeTasks.forEach { task ->
      if (task::class.java.name.contains("CompilationDependenciesResolutionTask")) {
        compilerManager.beforeTasks.remove(task)
      }
    }
  }
}
