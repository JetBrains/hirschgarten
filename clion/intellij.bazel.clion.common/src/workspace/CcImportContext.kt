package org.jetbrains.bazel.clion.workspace

import com.intellij.build.events.MessageEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterContext
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.TaskId
import java.nio.file.Path

internal interface CcImportContext {

  val project: Project

  val snapshot: WorkspaceSnapshot

  val execroot: Path

  fun reportEvent(severity: MessageEvent.Kind, message: @NlsSafe String, description: @NlsSafe String? = null)

  fun resolve(location: OutputLocation): Path?

  companion object {

    fun create(taskId: TaskId, ctx: WorkspaceImporterContext, snapshot: WorkspaceSnapshot): CcImportContext {
      return CcImportContextImpl(taskId, ctx, snapshot)
    }
  }
}

private class CcImportContextImpl(
  private val taskId: TaskId,
  private val ctx: WorkspaceImporterContext,
  override val snapshot: WorkspaceSnapshot,
) : CcImportContext {

  override val project: Project
    get() = ctx.project

  override val execroot: Path
    get() = ctx.bazelInfo.execRoot

  override fun reportEvent(severity: MessageEvent.Kind, message: @NlsSafe String, description: @NlsSafe String?) {
    ctx.taskConsole.addDiagnosticMessage(taskId, message = message, description = description, severity = severity)
  }

  override fun resolve(location: OutputLocation): Path? {
    return ctx.outputResolver.resolve(location)
  }
}
