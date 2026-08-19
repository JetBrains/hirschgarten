package org.jetbrains.bazel.sync.projectStructure

import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.diagnostic.telemetry.helpers.use
import com.intellij.platform.diagnostic.telemetry.helpers.useWithScope
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.workspaceModel.ide.impl.WorkspaceModelImpl
import org.jetbrains.bazel.config.BazelBackendBundle
import org.jetbrains.bazel.performance.bspTracer
import org.jetbrains.bazel.progress.syncConsole
import org.jetbrains.bazel.progress.withSubtask
import org.jetbrains.bazel.workspacemodel.entities.BazelEntitySource
import org.jetbrains.bsp.protocol.TaskId

internal class ProjectModelApplicationTask(
  private val project: Project,
  private val taskId: TaskId,
  private val postActions: List<suspend () -> Unit>,
) {
  companion object {
    private const val MAX_REPLACE_WSM_ATTEMPTS = 3
  }

  suspend fun apply(storage: MutableEntityStorage) {
    project.syncConsole.withSubtask(
      subtaskId = taskId.subTask("apply-changes-on-workspace-model"),
      message = BazelBackendBundle.message("console.task.model.apply.changes"),
    ) {
      bspTracer.spanBuilder("apply.changes.on.workspace.model.ms").useWithScope {
        val workspaceModel = project.serviceAsync<WorkspaceModel>() as WorkspaceModelImpl
        workspaceModel.updateWithRetry(
          BazelBackendBundle.message("console.task.model.apply.changes.attempt.0.1.wsm", 0, 0),
          MAX_REPLACE_WSM_ATTEMPTS,
        ) { builder ->
          bspTracer.spanBuilder("replaceprojectmodel.in.apply.on.workspace.model.ms").use {
            builder.replaceBySource(
              sourceFilter = { entitySource -> entitySource is BazelEntitySource },
              replaceWith = storage,
            )
          }
        }
      }
    }

    postActions.forEach { it() }
  }
}
