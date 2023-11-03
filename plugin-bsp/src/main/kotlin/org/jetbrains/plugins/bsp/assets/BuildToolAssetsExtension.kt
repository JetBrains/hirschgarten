package org.jetbrains.plugins.bsp.assets

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.plugins.bsp.flow.open.WithBuildToolId
import javax.swing.Icon

public interface BuildToolAssetsExtension : WithBuildToolId {
  public val presentableName: String

  public val icon: Icon

  public val loadedTargetIcon: Icon
  public val unloadedTargetIcon: Icon

  public val invalidTargetIcon: Icon

  public companion object {
    internal val ep =
      ExtensionPointName.create<BuildToolAssetsExtension>("com.intellij.buildToolAssetsExtension")
  }
}
