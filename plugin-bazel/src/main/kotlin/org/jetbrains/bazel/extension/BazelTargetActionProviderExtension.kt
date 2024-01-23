package org.jetbrains.bazel.extension

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.bazel.ui.widgets.BazelBspJumpToBuildFileAction
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.extension.points.BuildToolId
import org.jetbrains.plugins.bsp.extension.points.BuildToolWindowTargetActionProviderExtension
import javax.swing.JComponent

class BazelTargetActionProviderExtension : BuildToolWindowTargetActionProviderExtension {
  override val buildToolId: BuildToolId = bazelBspBuildToolId

  override fun getTargetActions(
    component: JComponent,
    project: Project,
    buildTargetInfo: BuildTargetInfo,
  ): List<AnAction> =
    listOf(BazelBspJumpToBuildFileAction(component, project, buildTargetInfo))
}
