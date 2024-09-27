package org.jetbrains.plugins.bsp.impl.flow.sync.actions

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import org.jetbrains.bsp.protocol.jpsCompilation.utils.JpsFeatureFlags
import org.jetbrains.plugins.bsp.action.SuspendableAction
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.config.buildToolIdOrNull
import org.jetbrains.plugins.bsp.impl.flow.sync.PartialProjectSync
import org.jetbrains.plugins.bsp.impl.flow.sync.ProjectSyncTask
import org.jetbrains.plugins.bsp.impl.projectAware.isSyncInProgress

class ResyncTargetAction(private val targetId: BuildTargetIdentifier) :
  SuspendableAction({ BspPluginBundle.message("target.partial.sync.action.text") }, BspPluginIcons.reload) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    val syncScope = PartialProjectSync(targetsToSync = listOf(targetId))
    ProjectSyncTask(project).sync(syncScope = syncScope, buildProject = false)
  }

  override fun update(project: Project, e: AnActionEvent) {
    // TODO: https://youtrack.jetbrains.com/issue/BAZEL-1237
    val isBazelBsp = project.buildToolIdOrNull == BuildToolId("bazelbsp") // for now it's visible only for bazel-bsp projects
    // for now we dont support jps modules (TODO: https://youtrack.jetbrains.com/issue/BAZEL-1238)
    val isJpsDisabled = !JpsFeatureFlags.isJpsCompilationEnabled
    e.presentation.isVisible = isBazelBsp && isJpsDisabled
    e.presentation.isEnabled = !project.isSyncInProgress()
  }
}
