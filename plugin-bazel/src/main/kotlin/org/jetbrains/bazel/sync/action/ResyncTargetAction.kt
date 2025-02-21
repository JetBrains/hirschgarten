package org.jetbrains.bazel.sync.action

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.config.BspFeatureFlags
import org.jetbrains.bazel.config.BspPluginBundle
import org.jetbrains.bazel.config.BspPluginIcons
import org.jetbrains.bazel.config.BuildToolId
import org.jetbrains.bazel.config.buildToolIdOrNull
import org.jetbrains.bazel.sync.scope.PartialProjectSync
import org.jetbrains.bazel.sync.status.isSyncInProgress
import org.jetbrains.bazel.sync.task.ProjectSyncTask
import org.jetbrains.bsp.protocol.jpsCompilation.utils.JpsFeatureFlags

class ResyncTargetAction private constructor(private val targetId: BuildTargetIdentifier) :
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
      e.presentation.isVisible = BspFeatureFlags.enablePartialSync && isBazelBsp && isJpsDisabled
      e.presentation.isEnabled = !project.isSyncInProgress()
    }

    companion object {
      fun createIfEnabled(targetId: BuildTargetIdentifier): ResyncTargetAction? =
        if (BspFeatureFlags.enablePartialSync) {
          ResyncTargetAction(targetId)
        } else {
          null
        }
    }
  }
