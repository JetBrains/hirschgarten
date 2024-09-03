package org.jetbrains.plugins.bsp.extensionPoints

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.WithBuildToolId
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.config.withBuildToolId
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo
import javax.swing.JComponent

public interface BuildToolWindowTargetActionProviderExtension : WithBuildToolId {
  public fun getTargetActions(
    component: JComponent,
    project: Project,
    buildTargetInfo: BuildTargetInfo,
  ): List<AnAction>

  public companion object {
    val ep: ExtensionPointName<BuildToolWindowTargetActionProviderExtension> =
      ExtensionPointName.create("org.jetbrains.bsp.buildToolWindowTargetActionProviderExtension")
  }
}

public val Project.targetActionProvider: BuildToolWindowTargetActionProviderExtension?
  get() = BuildToolWindowTargetActionProviderExtension.ep.withBuildToolId(buildToolId)
