package org.jetbrains.bazel.clion.workspace

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.NlsContexts
import com.jetbrains.cidr.lang.workspace.OCWorkspace
import com.jetbrains.cidr.lang.workspace.OCWorkspaceImpl
import org.jetbrains.annotations.PropertyKey
import org.jetbrains.bazel.clion.BazelClionBundle
import org.jetbrains.bazel.progress.withSubtask
import org.jetbrains.bazel.sync.workspace.importer.BazelWorkspaceImporter
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterContext
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterPhase
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterResult
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot

private const val CLIENT_KEY = "BAZEL_CC"
private const val CLIENT_VERSION = 0

private val log = logger<CcWorkspaceImporter>()

internal class CcWorkspaceImporter : BazelWorkspaceImporter, BazelWorkspaceImporter.Named {

  private var configurations: List<CcResolveConfiguration> = emptyList()

  override val importerName: @NlsContexts.ProgressTitle String
    get() = BazelClionBundle.message("cc.workspace.importer.name")

  override suspend fun import(
    context: WorkspaceImporterContext,
    phase: WorkspaceImporterPhase,
    snapshot: WorkspaceSnapshot,
  ): Result<WorkspaceImporterResult> = when (phase) {
    is WorkspaceImporterPhase.Initialize -> onInitialize(context, snapshot)
    is WorkspaceImporterPhase.WorkspaceApply -> onWorkspaceApply(phase)
    is WorkspaceImporterPhase.PostProcessing -> onPostProcessing(context, snapshot)
    else -> Result.success(WorkspaceImporterResult.Success)
  }

  private suspend fun onInitialize(ctx: WorkspaceImporterContext, snapshot: WorkspaceSnapshot): Result<WorkspaceImporterResult> {
    val target2Toolchain = subtask(ctx, snapshot, "cc.import.task.toolchain.map") { buildToolchainMap() }
    if (target2Toolchain.isEmpty()) return Result.success(WorkspaceImporterResult.Abort)

    val toolchain2Compiler = subtask(ctx, snapshot, "cc.import.task.compiler.settings") { buildCompilerSettings() }
    val target2Compiler = target2Toolchain.mapValues { toolchain2Compiler[it.value] }

    configurations = subtask(ctx, snapshot, "cc.import.task.equivalence.classes") { buildEquivalenceClasses(target2Compiler) }

    return Result.success(WorkspaceImporterResult.Success)
  }

  private fun onWorkspaceApply(phase: WorkspaceImporterPhase.WorkspaceApply): Result<WorkspaceImporterResult> {
    addCcWorkspaceModule(phase.builder, phase.entitySource)
    return Result.success(WorkspaceImporterResult.Success)
  }

  private suspend fun onPostProcessing(ctx: WorkspaceImporterContext, snapshot: WorkspaceSnapshot): Result<WorkspaceImporterResult> {
    // no module means the workspace apply dropped it, so the entity write would find nothing
    if (findCcWorkspaceModuleId(ctx.project) == null) {
      log.error("the module `$CC_WORKSPACE_MODULE_NAME` is absent, so the CC import drops ${configurations.size} configuration(s)")
      return Result.success(WorkspaceImporterResult.Abort)
    }

    val workspace = OCWorkspaceImpl.getInstanceImpl(ctx.project).getModifiableModel(CLIENT_KEY, clear = true)
    workspace.setClientVersion(CLIENT_VERSION)

    subtask(ctx, snapshot, "cc.import.task.oc.workspace") { buildWorkspaceModel(configurations, workspace) }
    subtask(ctx, snapshot, "cc.import.task.compiler.info") { collectCompilerInfo(workspace) }

    workspace.preCommit()
    workspace.commitAndContribute()

    return Result.success(WorkspaceImporterResult.Success)
  }

  private suspend fun <T> subtask(
    ctx: WorkspaceImporterContext,
    snapshot: WorkspaceSnapshot,
    key: @PropertyKey(resourceBundle = BazelClionBundle.BUNDLE_FQN) String,
    body: suspend context(CcImportContext) () -> T,
  ): T {
    return ctx.taskConsole.withSubtask(ctx.taskId.subTask(key), BazelClionBundle.message(key)) { taskId ->
      val taskCtx = CcImportContext.create(taskId, ctx, snapshot)
      body(taskCtx)
    }
  }
}
