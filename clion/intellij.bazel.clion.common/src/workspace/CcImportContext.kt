package org.jetbrains.bazel.clion.workspace

import com.intellij.build.events.MessageEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterContext
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.target.isWorkspace
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.OutputLocationParser
import org.jetbrains.bsp.protocol.OutputLocationResolver
import org.jetbrains.bsp.protocol.TaskId
import java.nio.file.Path

@ApiStatus.Internal
interface CcImportContext {

  val taskId: TaskId

  val project: Project

  val vfuManager: VirtualFileUrlManager

  val snapshot: WorkspaceSnapshot

  val execroot: Path

  fun reportEvent(severity: MessageEvent.Kind, message: @NlsSafe String, description: @NlsSafe String? = null)

  val outputResolver: OutputLocationResolver

  val outputParser: OutputLocationParser

  companion object {

    fun create(taskId: TaskId, ctx: WorkspaceImporterContext, snapshot: WorkspaceSnapshot): CcImportContext {
      return CcImportContextImpl(ctx, taskId, snapshot)
    }
  }
}

/**
 * The targets that get a resolve configuration: those of the main repository and of the local repositories.
 * External and toolchain targets get none, because their configurations would be many times more than those of the
 * project, and a configuration makes its files project code.
 */
@ApiStatus.Internal
fun WorkspaceSnapshot.importedTargets(): Sequence<BuildTarget> =
  targets.allTargets().filter { it.isWorkspace(repoMapping) }

private class CcImportContextImpl(
  private val ctx: WorkspaceImporterContext,
  override val taskId: TaskId,
  override val snapshot: WorkspaceSnapshot,
) : CcImportContext {

  override val project: Project
    get() = ctx.project

  override val vfuManager: VirtualFileUrlManager
    get() = ctx.vfuManager

  override val execroot: Path
    get() = ctx.bazelInfo.execRoot

  override fun reportEvent(severity: MessageEvent.Kind, message: @NlsSafe String, description: @NlsSafe String?) {
    ctx.taskConsole.addDiagnosticMessage(taskId, message = message, description = description, severity = severity)
  }

  override val outputResolver: OutputLocationResolver
    get() = ctx.outputResolver

  override val outputParser: OutputLocationParser
    get() = ctx.outputParser
}
