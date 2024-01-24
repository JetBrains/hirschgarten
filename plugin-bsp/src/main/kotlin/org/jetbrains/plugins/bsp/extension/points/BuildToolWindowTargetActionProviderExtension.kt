package org.jetbrains.plugins.bsp.extension.points

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import javax.swing.JComponent

public interface BuildToolWindowTargetActionProviderExtension : WithBuildToolId {
  public fun getTargetActions(component: JComponent, project: Project, buildTargetInfo: BuildTargetInfo): List<AnAction>

  public companion object {
    internal val ep: ExtensionPointName<BuildToolWindowTargetActionProviderExtension> =
      ExtensionPointName.create("org.jetbrains.bsp.buildToolWindowTargetActionProviderExtension")
  }
}
