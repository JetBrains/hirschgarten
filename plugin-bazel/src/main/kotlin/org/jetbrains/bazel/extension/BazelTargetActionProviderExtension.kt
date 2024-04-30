package org.jetbrains.bazel.extension

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.bazel.debug.bsp.StarlarkDebugAction
import org.jetbrains.bazel.ui.widgets.BazelBspJumpToBuildFileAction
import org.jetbrains.plugins.bsp.extension.points.BuildToolId
import org.jetbrains.plugins.bsp.extension.points.BuildToolWindowTargetActionProviderExtension
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import javax.swing.JComponent

class BazelTargetActionProviderExtension : BuildToolWindowTargetActionProviderExtension {
  override val buildToolId: BuildToolId = bazelBspBuildToolId

  override fun getTargetActions(
    component: JComponent,
    project: Project,
    buildTargetInfo: BuildTargetInfo,
  ): List<AnAction> =
    listOfNotNull(
      BazelBspJumpToBuildFileAction(component, project, buildTargetInfo),
      if (StarlarkDebugAction.isApplicableTo(buildTargetInfo)) StarlarkDebugAction(buildTargetInfo.id) else null,
    )
}
