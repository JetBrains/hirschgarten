package org.jetbrains.bazel.clion.workspace

import com.intellij.openapi.application.writeAction
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

internal class CcWorkspaceImporter : BazelWorkspaceImporter, BazelWorkspaceImporter.Named {

  lateinit var workspace: OCWorkspace.ModifiableModel

  override val importerName: @NlsContexts.ProgressTitle String
    get() = BazelClionBundle.message("cc.workspace.importer.name")

  override suspend fun import(
    context: WorkspaceImporterContext,
    phase: WorkspaceImporterPhase,
    snapshot: WorkspaceSnapshot,
  ): Result<WorkspaceImporterResult> = when (phase) {
    is WorkspaceImporterPhase.Initialize -> onInitialize(context, snapshot)
    is WorkspaceImporterPhase.WorkspaceApply -> onWorkspaceApply(context, snapshot)
    else -> Result.success(WorkspaceImporterResult.Success)
  }

  private suspend fun onInitialize(ctx: WorkspaceImporterContext, snapshot: WorkspaceSnapshot): Result<WorkspaceImporterResult> {
    val target2Toolchain = subtask(ctx, snapshot, "cc.import.task.toolchain.map") { buildToolchainMap() }
    if (target2Toolchain.isEmpty()) return Result.success(WorkspaceImporterResult.Abort)

    val toolchain2Compiler = subtask(ctx, snapshot, "cc.import.task.compiler.settings") { buildCompilerSettings() }
    val target2Compiler = target2Toolchain.mapValues { toolchain2Compiler[it.value] }

    val configurations = subtask(ctx, snapshot, "cc.import.task.equivalence.classes") { buildEquivalenceClasses(target2Compiler) }

    workspace = OCWorkspaceImpl.getInstanceImpl(ctx.project).getModifiableModel(CLIENT_KEY, clear = true)
    // TODO: how to dispose the model? Do we need to dispose it?

    subtask(ctx, snapshot, "cc.import.task.oc.workspace") {
      buildWorkspaceModel(configurations, workspace)
    }

    return Result.success(WorkspaceImporterResult.Success)
  }

  private suspend fun onWorkspaceApply(ctx: WorkspaceImporterContext, snapshot: WorkspaceSnapshot): Result<WorkspaceImporterResult> {
    workspace.setClientVersion(CLIENT_VERSION)

    subtask(ctx, snapshot, "cc.import.task.compiler.info") { collectCompilerInfo(workspace) }
    workspace.preCommit()

    writeAction {
      // TODO: should we use commitAndContribute here instead?
      workspace.commit()
    }

    return Result.success(WorkspaceImporterResult.Success)
  }

  private suspend fun <T> subtask(
    ctx: WorkspaceImporterContext,
    snapshot: WorkspaceSnapshot,
    key: @PropertyKey(resourceBundle = BazelClionBundle.BUNDLE_FQN) String,
    body: suspend context(CcImportContext) () -> T,
  ): T {
    return ctx.taskConsole.withSubtask(ctx.progressReporter, ctx.taskId.subTask(key), BazelClionBundle.message(key)) { taskId ->
      val taskCtx = CcImportContext.create(taskId, ctx, snapshot)
      body(taskCtx)
    }
  }
}
