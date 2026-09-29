package org.jetbrains.bazel.sync.workspace.importer

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.autolink.mapExtensionSafe
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.intellij.platform.workspace.storage.MutableEntityStorage
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.config.BazelBackendBundle
import org.jetbrains.bazel.progress.TaskConsole
import org.jetbrains.bazel.progress.syncConsole
import org.jetbrains.bazel.progress.withSubtask
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectEntitySource
import org.jetbrains.bsp.protocol.OutputLocationParser
import org.jetbrains.bsp.protocol.OutputLocationResolver
import org.jetbrains.bsp.protocol.TaskId

@ApiStatus.Internal
class WorkspaceImporterHelper(
  private val project: Project,
  private val taskConsole: TaskConsole,
  private val progressReporter: SequentialProgressReporter,
  private val builder: MutableEntityStorage,
  private val outputResolver: OutputLocationResolver,
  private val outputParser: OutputLocationParser,
  private val bazelInfo: BazelInfo,
) {
  companion object {
    private val log = logger<WorkspaceImporterHelper>()
  }

  private val workspaceModel = WorkspaceModel.getInstance(project)
  private val toSkip = mutableSetOf<BazelWorkspaceImporter>()
  private lateinit var importers: List<BazelWorkspaceImporter>

  suspend fun invoke(reporter: SequentialProgressReporter, snapshot: WorkspaceSnapshot, taskId: TaskId) {
    val context = WorkspaceImporterContext(
      project = project,
      taskConsole = taskConsole,
      progressReporter = progressReporter,
      vfuManager = workspaceModel.getVirtualFileUrlManager(),
      currentSnapshot = workspaceModel.currentSnapshot,
      outputResolver = outputResolver,
      outputParser = outputParser,
      bazelInfo = bazelInfo,
    )
    importers = BazelWorkspaceImporterFactory.EP_NAME.mapExtensionSafe { it.createWorkspaceImporter(context) }
    val namingBuilder = GlobalNamingContextBuilder.create(snapshot.repoMapping)

    taskConsole.withSubtask(
      reporter, taskId.subTask("workspace-importers"),
      BazelBackendBundle.message("bazel.workspace.importer.task.name"),
    ) { taskId ->
      taskConsole.withSubtask(
        subtaskId = taskId.subTask("workspace-importers-init"),
        message = BazelBackendBundle.message("workspace.importer.phase.initialization.progress"),
      ) { taskId ->
        importers.forEach { importer ->
          importer.runContextual(taskId, WorkspaceImporterPhase.Initialize(namingBuilder), snapshot)
            .onFailure { toSkip += importer }
            .onSuccess { result ->
              when (result) {
                WorkspaceImporterResult.Abort -> toSkip += importer
                WorkspaceImporterResult.Success -> {
                  /* noop */
                }
              }

            }
        }
      }

      val naming = namingBuilder.build()

      taskConsole.withSubtask(
        subtaskId = taskId.subTask("workspace-importers-wsm-building"),
        message = BazelBackendBundle.message("build.workspace.model"),
      ) { taskId ->
        importers.forEach { importer ->
          if (importer in toSkip) {
            return@forEach
          }
          importer.runContextual(taskId, WorkspaceImporterPhase.WorkspaceApply(builder, BazelProjectEntitySource, naming), snapshot)
            .onFailure { toSkip += importer }
            .onSuccess { result ->
              when (result) {
                WorkspaceImporterResult.Abort -> toSkip += importer
                WorkspaceImporterResult.Success -> { /* noop */
                }
              }
            }
        }
      }

      taskConsole.withSubtask(
        subtaskId = taskId.subTask("workspace-importers-finalize"),
        message = BazelBackendBundle.message("workspace.importer.phase.finalization"),
      ) { taskId ->
        importers.forEach { importer ->
          if (importer in toSkip) {
            return@forEach
          }
          importer.runContextual(taskId, WorkspaceImporterPhase.Finalize, snapshot)
            .onFailure { /* noop */ }
            .onSuccess { result ->
              when (result) {
                WorkspaceImporterResult.Abort -> toSkip += importer
                WorkspaceImporterResult.Success -> {
                  /* noop */
                }
              }
            }
        }
      }
    }
  }

  suspend fun invokeLate(reporter: SequentialProgressReporter, snapshot: WorkspaceSnapshot, taskId: TaskId) {
    taskConsole.withSubtask(
      reporter, taskId.subTask("workspace-importers-post-apply"),
      BazelBackendBundle.message("bazel.workspace.post.apply.task.name"),
    ) { taskId ->
      importers.forEach { importer ->
        if (importer in toSkip) {
          return@forEach
        }
        importer.runContextual(taskId, WorkspaceImporterPhase.PostProcessing, snapshot)
      }
    }
  }

  private fun <T> Result<T>.logExceptionIfNeeded(taskId: TaskId): Result<T> =
    this.onFailure { throwable ->
      log.error(throwable)
      project.syncConsole.finishSubtask(
        taskId,
        null,
        FailureResultImpl(throwable),
      )
    }

  // MAYBE RC: using context parameters for `TaskId` would be great fit here
  private suspend fun BazelWorkspaceImporter.runContextual(
    taskId: TaskId,
    phase: WorkspaceImporterPhase,
    snapshot: WorkspaceSnapshot,
  ): Result<WorkspaceImporterResult> = runCatching {
    if (this is BazelWorkspaceImporter.Named) {
      taskConsole.withSubtask(
        taskId.uniqueSubTask("importer"),
        BazelBackendBundle.message("workspace.importer.phase.executing", this.importerName),
      ) { importerTaskId ->
        this.import(phase, snapshot, importerTaskId)
      }
    }
    else {
      this.import(phase, snapshot, taskId)
    }
  }
    .fold(
      onSuccess = { it },
      onFailure = { Result.failure(it) },
    )
    .logExceptionIfNeeded(taskId)
}
