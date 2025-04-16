package org.jetbrains.bazel.sync.task

import com.intellij.openapi.project.Project
import kotlinx.coroutines.coroutineScope
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.sync.scope.FirstPhaseSync
import org.jetbrains.bazel.sync.scope.PartialProjectSync
import org.jetbrains.bazel.sync.scope.ProjectSyncScope
import org.jetbrains.bazel.ui.console.ids.BASE_PROJECT_SYNC_SUBTASK_ID
import org.jetbrains.bazel.ui.console.syncConsole
import org.jetbrains.bazel.ui.console.withSubtask
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsFirstPhaseParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsPartialParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult

class BaseProjectSync(private val project: Project) {
  suspend fun execute(
    syncScope: ProjectSyncScope,
    buildProject: Boolean,
    server: JoinedBuildServer,
    taskId: String,
  ): WorkspaceBuildTargetsResult =
    project.syncConsole.withSubtask(
      taskId = taskId,
      subtaskId = BASE_PROJECT_SYNC_SUBTASK_ID,
      message = BazelPluginBundle.message("console.task.base.sync"),
    ) {
      queryWorkspaceBuildTargets(server, syncScope, buildProject, taskId)
    }

  private suspend fun queryWorkspaceBuildTargets(
    server: JoinedBuildServer,
    syncScope: ProjectSyncScope,
    buildProject: Boolean,
    taskId: String,
  ): WorkspaceBuildTargetsResult =
    coroutineScope {
      // TODO: https://youtrack.jetbrains.com/issue/BAZEL-1237
      // PartialProjectSync is used only in ResyncTargetAction, which is visible only for bazel-bsp project
      if (syncScope is PartialProjectSync) {
        query("workspace/buildTargetsPartial") {
          server.workspaceBuildTargetsPartial(WorkspaceBuildTargetsPartialParams(syncScope.targetsToSync))
        }
      } else if (syncScope is FirstPhaseSync) {
        // TODO: https://youtrack.jetbrains.com/issue/BAZEL-1555
        query(
          "workspace/buildTargetsFirstPhase",
        ) { server.workspaceBuildTargetsFirstPhase(WorkspaceBuildTargetsFirstPhaseParams(taskId)) }
      } else if (buildProject) {
        query("workspace/buildAndGetBuildTargets") {
          server.workspaceBuildAndGetBuildTargets(WorkspaceBuildTargetsParams(taskId))
        }
      } else {
        query("workspace/buildTargets") { server.workspaceBuildTargets(WorkspaceBuildTargetsParams(taskId)) }
      }
    }
}
