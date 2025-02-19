package org.jetbrains.bazel.assets

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.WithBuildToolId
import org.jetbrains.bazel.config.buildToolIdOrDefault
import org.jetbrains.bazel.config.withBuildToolIdOrDefault
import javax.swing.Icon

public interface BuildToolAssetsExtension : WithBuildToolId {
  public val presentableName: String

  /** Target icon, shown next to individual targets in the tool window */
  public val targetIcon: Icon

  /**
   * Icon for target-related errors, including invalid targets.
   * Can be equal to `AllIcons.General.Error` if a suitable icon is unavailable.
   */
  public val errorTargetIcon: Icon

  /** Icon for the tool window */
  public val toolWindowIcon: Icon

  public companion object {
    internal val ep =
      ExtensionPointName.create<BuildToolAssetsExtension>("org.jetbrains.bazel.buildToolAssetsExtension")
  }
}

public val Project.assets: BuildToolAssetsExtension
  get() = BuildToolAssetsExtension.ep.withBuildToolIdOrDefault(buildToolIdOrDefault)
