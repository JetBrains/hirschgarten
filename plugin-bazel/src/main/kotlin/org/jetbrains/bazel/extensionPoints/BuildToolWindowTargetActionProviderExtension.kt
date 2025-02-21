package org.jetbrains.bazel.extensionPoints

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.WithBuildToolId
import org.jetbrains.bazel.config.buildToolId
import org.jetbrains.bazel.config.withBuildToolId
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo
import javax.swing.JComponent

public interface BuildToolWindowTargetActionProviderExtension : WithBuildToolId {
  public fun getTargetActions(
    component: JComponent,
    project: Project,
    buildTargetInfo: BuildTargetInfo,
  ): List<AnAction>

  public companion object {
    val ep: ExtensionPointName<BuildToolWindowTargetActionProviderExtension> =
      ExtensionPointName.create("org.jetbrains.bazel.buildToolWindowTargetActionProviderExtension")
  }
}

public val Project.targetActionProvider: BuildToolWindowTargetActionProviderExtension?
  get() = BuildToolWindowTargetActionProviderExtension.ep.withBuildToolId(buildToolId)
