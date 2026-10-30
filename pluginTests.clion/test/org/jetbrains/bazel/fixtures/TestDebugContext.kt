package org.jetbrains.bazel.fixtures

import com.intellij.build.events.MessageEvent
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.clion.run.debug.CcDebugContext
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.state.GenericRunConfigurationState
import org.jetbrains.bazel.run.state.GenericRunState
import org.jetbrains.bsp.protocol.TaskGroupId
import org.jetbrains.bsp.protocol.TaskId

internal suspend fun <T> withTestDebugContext(
  project: Project,
  label: Label,
  body: suspend context(CcDebugContext) () -> T,
): Pair<TestDebugContext, T> {
  val ctx = TestDebugContext(project, label)
  return ctx to body(ctx)
}

internal class TestDebugContext(
  override val project: Project,
  override val label: Label,
) : CcDebugContext {

  override val taskId: TaskId
    get() = TaskGroupId("test").task("task")

  override val state: GenericRunConfigurationState = GenericRunState()

  val events: MutableList<TestEvent> = mutableListOf()

  override fun reportEvent(severity: MessageEvent.Kind, message: String, description: String?) {
    events += TestEvent(severity, message, description)
  }
}
