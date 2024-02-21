package org.jetbrains.plugins.bsp.services

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.extension.points.WithBuildToolId

// BAZEL-831: suuuper temporary EP, later will become more generic
public interface InvalidTargetsProviderExtension : WithBuildToolId {
  public fun provideInvalidTargets(project: Project): List<BuildTargetIdentifier>

  public companion object {
    internal val ep =
      ExtensionPointName.create<InvalidTargetsProviderExtension>("org.jetbrains.bsp.invalidTargetsProviderExtension")
  }
}
