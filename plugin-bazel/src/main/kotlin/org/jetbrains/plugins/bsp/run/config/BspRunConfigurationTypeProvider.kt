package org.jetbrains.plugins.bsp.run.config

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.WithBuildToolId
import org.jetbrains.plugins.bsp.config.buildToolIdOrDefault
import org.jetbrains.plugins.bsp.config.withBuildToolIdOrDefault

/**
 * Provides the type of run configuration for BSP, containing the icon and the name of the build tool.
 */
interface BspRunConfigurationTypeProvider : WithBuildToolId {
  val runConfigurationType: BspRunConfigurationType

  companion object {
    val ep =
      ExtensionPointName.create<BspRunConfigurationTypeProvider>("org.jetbrains.bsp.runConfigurationTypeProvider")

    fun getConfigurationType(project: Project): BspRunConfigurationType =
      ep.withBuildToolIdOrDefault(project.buildToolIdOrDefault).runConfigurationType
  }
}
