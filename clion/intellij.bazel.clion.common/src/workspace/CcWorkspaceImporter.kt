package org.jetbrains.bazel.clion.workspace

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.NlsContexts
import com.jetbrains.cidr.lang.workspace.OCWorkspaceImpl
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.PropertyKey
import org.jetbrains.bazel.clion.BazelCLionCommonBundle
import org.jetbrains.bazel.clion.BazelCLionFeatureFlags
import org.jetbrains.bazel.progress.withSubtask
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporterFactory
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterContext
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterPhase
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterResult
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bsp.protocol.TaskId

@ApiStatus.Internal
const val CC_CLIENT_KEY: String = "BAZEL_CC"

private const val CLIENT_VERSION = 0

private val LOG = logger<CcWorkspaceImporter>()

internal class CcWorkspaceImporter(val context: WorkspaceImporterContext) : BazelWorkspaceImporter, BazelWorkspaceImporter.Named {

  class Factory : BazelWorkspaceImporterFactory {
    override fun createWorkspaceImporter(context: WorkspaceImporterContext): BazelWorkspaceImporter =
      CcWorkspaceImporter(context)
  }

  private var configurations: List<CcResolveConfiguration> = emptyList()

  override val importerName: @NlsContexts.ProgressTitle String
    get() = BazelCLionCommonBundle.message("cc.workspace.importer.name")

  override suspend fun import(phase: WorkspaceImporterPhase, snapshot: WorkspaceSnapshot, taskId: TaskId): Result<WorkspaceImporterResult> {
    if (!BazelCLionFeatureFlags.isCLionEnabled) return Result.success(WorkspaceImporterResult.Abort)

    return when (phase) {
      is WorkspaceImporterPhase.Initialize -> onInitialize(snapshot, taskId)
      is WorkspaceImporterPhase.WorkspaceApply -> onWorkspaceApply(phase)
      is WorkspaceImporterPhase.PostProcessing -> onPostProcessing(snapshot, taskId)
      else -> Result.success(WorkspaceImporterResult.Success)
    }
  }

  private suspend fun onInitialize(snapshot: WorkspaceSnapshot, taskId: TaskId): Result<WorkspaceImporterResult> {
    val target2Toolchain = subtask(snapshot, "cc.import.task.toolchain.map", taskId) { buildToolchainMap() }
    if (target2Toolchain.isEmpty()) return Result.success(WorkspaceImporterResult.Abort)

    val toolchain2Compiler = subtask(snapshot, "cc.import.task.compiler.settings", taskId) { buildCompilerSettings() }
    val target2Compiler = target2Toolchain.mapValues { toolchain2Compiler[it.value] }

    configurations = subtask(snapshot, "cc.import.task.equivalence.classes", taskId) { buildEquivalenceClasses(target2Compiler) }

    return Result.success(WorkspaceImporterResult.Success)
  }

  private fun onWorkspaceApply(phase: WorkspaceImporterPhase.WorkspaceApply): Result<WorkspaceImporterResult> {
    addCcWorkspaceModule(phase.builder, phase.entitySource)
    return Result.success(WorkspaceImporterResult.Success)
  }

  private suspend fun onPostProcessing(snapshot: WorkspaceSnapshot, taskId: TaskId): Result<WorkspaceImporterResult> {
    if (findCcWorkspaceModuleId(context.project) == null) {
      LOG.error("CC workspace module is absent, dropping ${configurations.size} configuration(s)")
      return Result.success(WorkspaceImporterResult.Abort)
    }

    val workspace = OCWorkspaceImpl.getInstanceImpl(context.project).getModifiableModel(CC_CLIENT_KEY, clear = true)

    try {
      workspace.setClientVersion(CLIENT_VERSION)

      subtask(snapshot, "cc.import.task.oc.workspace", taskId) { buildWorkspaceModel(workspace, configurations) }
      subtask(snapshot, "cc.import.task.compiler.info", taskId) { collectCompilerInfo(workspace, configurations) }

      workspace.preCommit()
      workspace.commitAndContribute()
    }
    finally {
      Disposer.dispose(workspace)
      configurations = emptyList()
    }

    return Result.success(WorkspaceImporterResult.Success)
  }

  private suspend fun <T> subtask(
    snapshot: WorkspaceSnapshot,
    key: @PropertyKey(resourceBundle = BazelCLionCommonBundle.BUNDLE_FQN) String,
    parentTaskId: TaskId,
    body: suspend context(CcImportContext) () -> T,
  ): T {
    return context.taskConsole.withSubtask(parentTaskId.subTask(key), BazelCLionCommonBundle.message(key)) { taskId ->
      val taskCtx = CcImportContext.create(taskId, context, snapshot)
      body(taskCtx)
    }
  }
}
