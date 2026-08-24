package org.jetbrains.bazel.inspections

import com.intellij.ide.JAVA_DONT_CHECK_OUT_OF_SOURCE_FILES
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.jetbrains.bazel.config.isBazelProject

internal class JavaInspectionsSetup: ProjectActivity {
  override suspend fun execute(project: Project) {
    if (!project.isBazelProject) return

    // Switch off inspection "Java file is located outside of the module source root, so it won't be compiled"
    // Bazel defines its own source roots, and warns if source file does not belong to any target
    PropertiesComponent.getInstance(project).setValue(JAVA_DONT_CHECK_OUT_OF_SOURCE_FILES, true)
  }
}
