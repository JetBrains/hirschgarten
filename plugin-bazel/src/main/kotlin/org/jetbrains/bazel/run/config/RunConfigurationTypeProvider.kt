package org.jetbrains.bazel.run.config

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.WithBuildToolId
import org.jetbrains.bazel.config.buildToolIdOrDefault
import org.jetbrains.bazel.config.withBuildToolIdOrDefault

/**
 * Provides the type of run configuration for BSP, containing the icon and the name of the build tool.
 */
interface RunConfigurationTypeProvider : WithBuildToolId {
  val runConfigurationType: BspRunConfigurationType

  companion object {
    val ep =
      ExtensionPointName.create<RunConfigurationTypeProvider>("org.jetbrains.bazel.runConfigurationTypeProvider")

    fun getConfigurationType(project: Project): BspRunConfigurationType =
      ep.withBuildToolIdOrDefault(project.buildToolIdOrDefault).runConfigurationType
  }
}
