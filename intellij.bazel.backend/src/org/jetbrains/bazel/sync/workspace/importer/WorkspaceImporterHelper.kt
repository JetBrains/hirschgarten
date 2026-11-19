package org.jetbrains.bazel.sync.workspace.importer

import com.intellij.aspect.lib.INTELLIJ_ASPECT
import com.intellij.build.events.MessageEvent
import com.intellij.openapi.externalSystem.autolink.mapExtensionSafe
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.intellij.platform.workspace.storage.MutableEntityStorage
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.TargetCollection
import org.jetbrains.bazel.config.BazelBackendBundle
import org.jetbrains.bazel.languages.projectview.projectView
import org.jetbrains.bazel.progress.TaskConsole
import org.jetbrains.bazel.progress.isolateSubtaskFailure
import org.jetbrains.bazel.progress.withSubtask
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bazel.server.bsp.managers.BazelBspAspectsManager
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
  private val capabilities: WorkspaceEnvironmentCapabilities,
  private val server: BazelServerFacade,
) {
  private val bazelInfo = server.bazelInfo
  private val workspaceModel = WorkspaceModel.getInstance(project)
  private val toSkip = mutableSetOf<BazelWorkspaceImporter>()
  private lateinit var importers: List<BazelWorkspaceImporter>

  private val importerToTime = HashMap<String, Long>()

  /**
   * Time spent by each [BazelWorkspaceImporter] in milliseconds
   */
  val timePerImporter: Map<String, Long>
    get() = importerToTime

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
      capabilities = capabilities,
    )
    val rootTargets = snapshot.targetGraph.rootTargets
    importers = BazelWorkspaceImporterFactory.EP_NAME.mapExtensionSafe { it.createWorkspaceImporter(context) }
    val namingBuilder = GlobalNamingContextBuilder.create(snapshot.repoMapping)
    val toolOutputRequests = ToolOutputRequestsCollector()

    taskConsole.withSubtask(
      reporter, taskId.subTask("workspace-importers"),
      BazelBackendBundle.message("bazel.workspace.importer.task.name"),
    ) { taskId ->
      taskConsole.withSubtask(
        subtaskId = taskId.subTask("workspace-importers-init"),
        message = BazelBackendBundle.message("workspace.importer.phase.initialization.progress"),
      ) { taskId ->
        importers.forEach { importer ->
          val result = importer.runContextual(taskId, WorkspaceImporterPhase.Initialize(namingBuilder, toolOutputRequests), snapshot)
          if (result != WorkspaceImporterResult.Success) toSkip += importer
        }
      }

      val neededToolOutputs = toolOutputRequests.getOutputGroupRequests()
      val neededOutputFiles = toolOutputRequests.getOutputRequests()

      if (neededToolOutputs.isNotEmpty()) {
       taskConsole.withSubtask(
         subtaskId = taskId.subTask("workspace-importers-materialize-tools"),
         message = BazelBackendBundle.message("build.workspace.tools", neededToolOutputs.map { it.toString() }.joinToString(", "))
       )  {
         taskId ->
         val targetsSpec = TargetCollection(rootTargets.map {it.label}.distinct().toList())
         server.materializeExtraSyncOutputs(taskId, targetsSpec, neededToolOutputs.toList(), neededOutputFiles.toList())
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
    val startTime = System.currentTimeMillis()
    try {
      taskConsole.withSubtask(
        taskId.uniqueSubTask("importer"),
        BazelBackendBundle.message("workspace.importer.phase.executing", this.importerName),
      ) { importerTaskId ->
        this.import(phase, snapshot, importerTaskId)
      }
    }
    finally {
      val endTime = System.currentTimeMillis()
      val importerName = this.javaClass.name
      importerToTime[importerName] = (importerToTime[importerName] ?: 0L) + (endTime - startTime)
    }
  }
}
