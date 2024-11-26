package org.jetbrains.plugins.bsp.impl.flow.sync

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.reportSequentialProgress
import org.jetbrains.plugins.bsp.action.SuspendableAction
import org.jetbrains.plugins.bsp.building.syncConsole
import org.jetbrains.plugins.bsp.impl.flow.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.plugins.bsp.impl.server.connection.connection
import org.jetbrains.plugins.bsp.projectStructure.AllProjectStructuresProvider

class QuickySyncAction : SuspendableAction("XD") {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    QuickySync(project).sync()
  }
}

class QuickySync(private val project: Project) {
  suspend fun sync() {
    val aa = project.syncConsole
    val taskID = "qq-s"

    withBackgroundProgress(project, "Syncing project...", true) {
      reportSequentialProgress { reporter ->
        aa.startTask(taskID, "helllo", "XD")
        val diff = AllProjectStructuresProvider(project).newDiff()
        project.connection.runWithServer { server, capabilities ->
          val baseTargetInfos = BaseProjectSync(project).execute(FullQuickySync, false, server, capabilities, taskID)

          val environment =
            ProjectSyncHookEnvironment(
              project = project,
              server = server,
              capabilities = capabilities,
              diff = diff,
              taskId = PROJECT_SYNC_TASK_ID,
              progressReporter = reporter,
              baseTargetInfos = baseTargetInfos,
              syncScope = FullQuickySync,
            )

          project.defaultProjectSyncHooks.forEach {
            it.onSync(environment)
          }
          project.additionalProjectSyncHooks.forEach {
            it.onSync(environment)
          }
        }
        diff.applyAll(FullQuickySync, taskID)
        aa.finishTask(taskID, "XD")
      }
    }
  }
}
