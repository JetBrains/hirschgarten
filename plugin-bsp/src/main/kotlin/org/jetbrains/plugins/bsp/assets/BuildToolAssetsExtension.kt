package org.jetbrains.plugins.bsp.assets

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.extension.points.WithBuildToolId
import org.jetbrains.plugins.bsp.extension.points.withBuildToolIdOrDefault
import javax.swing.Icon

public interface BuildToolAssetsExtension : WithBuildToolId {
  public val presentableName: String

  public val icon: Icon

  public val loadedTargetIcon: Icon
  public val unloadedTargetIcon: Icon

  public val invalidTargetIcon: Icon

  public companion object {
    internal val ep =
      ExtensionPointName.create<BuildToolAssetsExtension>("org.jetbrains.bsp.buildToolAssetsExtension")
  }
}

public val Project.assets: BuildToolAssetsExtension
  get() = BuildToolAssetsExtension.ep.withBuildToolIdOrDefault(buildToolId)
