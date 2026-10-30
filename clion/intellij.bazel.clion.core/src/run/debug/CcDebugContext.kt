package org.jetbrains.bazel.clion.run.debug

import com.intellij.build.events.MessageEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.progress.TaskConsole
import org.jetbrains.bazel.run.state.GenericRunConfigurationState
import org.jetbrains.bsp.protocol.TaskId

@ApiStatus.Internal
interface CcDebugContext {

  val taskId: TaskId

  val project: Project

  /** The label of the target to debug. */
  val label: Label

  val state: GenericRunConfigurationState

  fun reportEvent(severity: MessageEvent.Kind, message: @NlsSafe String, description: @NlsSafe String? = null)

  companion object {

    fun create(
      taskId: TaskId,
      console: TaskConsole,
      project: Project,
      label: Label,
      state: GenericRunConfigurationState,
    ): CcDebugContext {
      return CcDebugContextImpl(taskId, console, project, label, state)
    }
  }
}

private class CcDebugContextImpl(
  override val taskId: TaskId,
  private val console: TaskConsole,
  override val project: Project,
  override val label: Label,
  override val state: GenericRunConfigurationState,
) : CcDebugContext {

  override fun reportEvent(severity: MessageEvent.Kind, message: @NlsSafe String, description: @NlsSafe String?) {
    console.addDiagnosticMessage(taskId, message = message, description = description, severity = severity)
  }
}
