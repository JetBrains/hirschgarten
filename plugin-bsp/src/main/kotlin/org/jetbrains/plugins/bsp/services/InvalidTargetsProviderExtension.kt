package org.jetbrains.plugins.bsp.services

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.WithBuildToolId
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.config.withBuildToolId

// BAZEL-831: suuuper temporary EP, later will become more generic
interface InvalidTargetsProviderExtension : WithBuildToolId {
  public fun provideInvalidTargets(project: Project): List<BuildTargetIdentifier>

  companion object {
    val ep =
      ExtensionPointName.create<InvalidTargetsProviderExtension>("org.jetbrains.bsp.invalidTargetsProviderExtension")
  }
}

val Project.invalidTargets: List<BuildTargetIdentifier>
  get() =
    InvalidTargetsProviderExtension.ep
      .withBuildToolId(buildToolId)
      ?.provideInvalidTargets(this)
      .orEmpty()
