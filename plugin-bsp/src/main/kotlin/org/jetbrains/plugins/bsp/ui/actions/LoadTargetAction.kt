package org.jetbrains.plugins.bsp.ui.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.assets.BuildToolAssetsExtension
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.extension.points.withBuildToolIdOrDefault
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import org.jetbrains.plugins.bsp.ui.notifications.BspBalloonNotifier

public class LoadTargetAction(
  private val targetId: BuildTargetId,
  text: () -> String,
  private val updateWidget: () -> Unit = {},
) : SuspendableAction(text), DumbAware {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    loadTarget(project, targetId)
    updateWidget()
  }

  public companion object {
    public suspend fun loadTarget(project: Project, targetId: BuildTargetId) {
      val magicMetaModel = MagicMetaModelService.getInstance(project).value
      val diff = magicMetaModel.loadTarget(targetId)
      diff?.applyOnWorkspaceModel()

      val assetsExtension = BuildToolAssetsExtension.ep.withBuildToolIdOrDefault(project.buildToolId)
      BspBalloonNotifier.info(
        title = assetsExtension.presentableName,
        content = BspPluginBundle.message("widget.load.target.notification", targetId),
        subtitle = BspPluginBundle.message("widget.load.target"),
        customIcon = assetsExtension.icon
      )
    }
  }
}
