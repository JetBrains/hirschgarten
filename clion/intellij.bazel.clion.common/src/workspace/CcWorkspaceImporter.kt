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
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.TaskId

@ApiStatus.Internal
const val CC_CLIENT_KEY: String = "BAZEL_CC"

private const val CLIENT_VERSION = 0

private val LOG = logger<CcWorkspaceImporter>()

internal class CcWorkspaceImporter(val context: WorkspaceImporterContext) : BazelWorkspaceImporter {

  class Factory : BazelWorkspaceImporterFactory {
    override fun createWorkspaceImporter(context: WorkspaceImporterContext): BazelWorkspaceImporter =
      CcWorkspaceImporter(context)
  }

  private var configurations: List<CcResolveConfiguration> = emptyList()
  private var toolchain2Compiler: Map<WorkspaceTargetKey, CcCompilerInfo> = emptyMap()

  /** The sync has no C/C++ target to import, but the model of a previous sync is still there. */
  private var clearPreviousModel = false

  override val importerName: @NlsContexts.ProgressTitle String
    get() = BazelCLionCommonBundle.message("cc.workspace.importer.name")

  override suspend fun import(phase: WorkspaceImporterPhase, snapshot: WorkspaceSnapshot, taskId: TaskId): WorkspaceImporterResult {
    if (!BazelCLionFeatureFlags.isCLionEnabled) return WorkspaceImporterResult.Abort

    return when (phase) {
      is WorkspaceImporterPhase.Initialize -> onInitialize(snapshot, phase, taskId)
      is WorkspaceImporterPhase.WorkspaceApply -> onWorkspaceApply(snapshot, phase, taskId)
      is WorkspaceImporterPhase.PostProcessing -> onPostProcessing(snapshot, taskId)
      else -> WorkspaceImporterResult.Success
    }
  }

  private suspend fun onInitialize(snapshot: WorkspaceSnapshot, phase: WorkspaceImporterPhase.Initialize, taskId: TaskId): WorkspaceImporterResult {
    subtask(snapshot, "cc.import.task.toolchain.request", taskId) { requestMissingCompilers(phase.toolOutputRequests)}
    return WorkspaceImporterResult.Success
  }

  private suspend fun onWorkspaceApply(snapshot: WorkspaceSnapshot, phase: WorkspaceImporterPhase.WorkspaceApply, taskId: TaskId): WorkspaceImporterResult {
    val target2Toolchain = subtask(snapshot, "cc.import.task.toolchain.map", taskId) { buildToolchainMap() }
    if (target2Toolchain.isEmpty()) {
      clearPreviousModel = OCWorkspaceImpl.getInstanceImpl(context.project).getConfigurations(CC_CLIENT_KEY).isNotEmpty()
      return if (clearPreviousModel) WorkspaceImporterResult.Success else WorkspaceImporterResult.Abort
    }

    val toolchain2Compiler = subtask(snapshot, "cc.import.task.compiler.settings", taskId) { buildCompilerSettings() }
    val target2Compiler = target2Toolchain.mapValues { toolchain2Compiler[it.value] }

    this.configurations = subtask(snapshot, "cc.import.task.equivalence.classes", taskId) { buildEquivalenceClasses(target2Compiler) }
    this.toolchain2Compiler = toolchain2Compiler

    addCcWorkspaceModule(phase.builder, phase.entitySource)
    addCcCompilerInfoEntities(phase.builder, phase.entitySource, toolchain2Compiler)

    return WorkspaceImporterResult.Success
  }

  private suspend fun onPostProcessing(snapshot: WorkspaceSnapshot, taskId: TaskId): WorkspaceImporterResult {
    if (clearPreviousModel) {
      clearWorkspaceModel()
      return WorkspaceImporterResult.Success
    }

    if (findCcWorkspaceModuleId(context.project) == null) {
      LOG.error("CC workspace module is absent, dropping ${configurations.size} configuration(s)")
      return WorkspaceImporterResult.Abort
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
      toolchain2Compiler = emptyMap()
    }

    return WorkspaceImporterResult.Success
  }

  private suspend fun clearWorkspaceModel() {
    val workspace = OCWorkspaceImpl.getInstanceImpl(context.project).getModifiableModel(CC_CLIENT_KEY, clear = true)
    try {
      workspace.preCommit()
      workspace.commitAndContribute()
    }
    finally {
      Disposer.dispose(workspace)
    }
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
