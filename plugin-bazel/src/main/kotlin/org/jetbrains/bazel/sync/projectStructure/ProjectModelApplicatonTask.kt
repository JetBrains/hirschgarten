package org.jetbrains.bazel.sync.projectStructure

import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.diagnostic.telemetry.helpers.use
import com.intellij.platform.diagnostic.telemetry.helpers.useWithScope
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.workspaceModel.ide.impl.WorkspaceModelImpl
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.performance.bspTracer
import org.jetbrains.bazel.progress.syncConsole
import org.jetbrains.bazel.progress.withSubtask
import org.jetbrains.bazel.sync.scope.FullProjectSync
import org.jetbrains.bazel.sync.scope.PartialProjectSync
import org.jetbrains.bazel.sync.scope.ProjectSyncScope
import org.jetbrains.bazel.workspacemodel.entities.BazelEntitySource
import org.jetbrains.bazel.workspacemodel.entities.BazelModuleEntitySource
import org.jetbrains.bsp.protocol.TaskId

internal class ProjectModelApplicatonTask(
  private val project: Project,
  private val scope: ProjectSyncScope,
  private val taskId: TaskId,
  private val postActions: List<suspend () -> Unit>,
) {
  companion object {
    private const val MAX_REPLACE_WSM_ATTEMPTS = 3
  }

  suspend fun apply(storage: MutableEntityStorage) {
    val sourceFilter: (EntitySource) -> Boolean =
      when (scope) {
        is FullProjectSync -> { entitySource -> entitySource is BazelEntitySource }
        is PartialProjectSync -> {
          val moduleNames =
            lazy {
              scope.targetsToSync
                .map { it.formatAsModuleName(project) }
            }

          object : (EntitySource) -> Boolean {
            override fun invoke(entitySource: EntitySource): Boolean =
              entitySource is BazelModuleEntitySource &&
              entitySource.moduleName in moduleNames.value
          }
        }
      }

    fun MutableEntityStorage.replaceBySource() {
      replaceBySource(
        sourceFilter = sourceFilter,
        replaceWith = storage,
      )
    }

    project.syncConsole.withSubtask(
      subtaskId = taskId.subTask("apply-changes-on-workspace-model"),
      message = BazelPluginBundle.message("console.task.model.apply.changes"),
    ) {
      bspTracer.spanBuilder("apply.changes.on.workspace.model.ms").useWithScope {
        val workspaceModel = project.serviceAsync<WorkspaceModel>() as WorkspaceModelImpl
        workspaceModel.updateWithRetry(
          BazelPluginBundle.message("console.task.model.apply.changes.attempt.0.1.wsm", 0, 0),
          MAX_REPLACE_WSM_ATTEMPTS,
        ) { builder ->
          bspTracer.spanBuilder("replaceprojectmodel.in.apply.on.workspace.model.ms").use {
            builder.replaceBySource()
          }
        }
      }
    }

    postActions.forEach { it() }
  }
}
