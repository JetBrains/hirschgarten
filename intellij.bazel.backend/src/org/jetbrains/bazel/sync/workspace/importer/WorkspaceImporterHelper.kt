package org.jetbrains.bazel.sync.workspace.importer

import com.intellij.openapi.externalSystem.autolink.mapExtensionSafe
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.intellij.platform.workspace.storage.MutableEntityStorage
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.config.BazelBackendBundle
import org.jetbrains.bazel.progress.TaskConsole
import org.jetbrains.bazel.progress.isolateSubtaskFailure
import org.jetbrains.bazel.progress.withSubtask
import org.jetbrains.bazel.sync.BazelOutFileHardLinks
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
  private val outFileHardLinks: BazelOutFileHardLinks,
  private val bazelInfo: BazelInfo,
) {
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
      outFileHardLinks = outFileHardLinks,
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
          val result = importer.runContextual(taskId, WorkspaceImporterPhase.Initialize(namingBuilder), snapshot)
          if (result != WorkspaceImporterResult.Success) toSkip += importer
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
          val result = importer.runContextual(taskId, WorkspaceImporterPhase.WorkspaceApply(builder, BazelProjectEntitySource, naming), snapshot)
          if (result != WorkspaceImporterResult.Success) toSkip += importer
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
          val result = importer.runContextual(taskId, WorkspaceImporterPhase.Finalize, snapshot)
          if (result == WorkspaceImporterResult.Abort) toSkip += importer
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

  // MAYBE RC: using context parameters for `TaskId` would be great fit here
  private suspend fun BazelWorkspaceImporter.runContextual(
    taskId: TaskId,
    phase: WorkspaceImporterPhase,
    snapshot: WorkspaceSnapshot,
  ): WorkspaceImporterResult? = taskConsole.isolateSubtaskFailure(taskId) {
    taskConsole.withSubtask(
      taskId.uniqueSubTask("importer"),
      BazelBackendBundle.message("workspace.importer.phase.executing", this.importerName),
    ) { importerTaskId ->
      this.import(phase, snapshot, importerTaskId)
    }
  }
}
