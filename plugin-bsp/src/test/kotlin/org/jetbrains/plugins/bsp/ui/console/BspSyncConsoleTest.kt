package org.jetbrains.plugins.bsp.ui.console

import com.intellij.build.BuildProgressListener
import com.intellij.build.events.BuildEvent
import com.intellij.build.events.impl.FinishBuildEventImpl
import com.intellij.build.events.impl.OutputBuildEventImpl
import com.intellij.build.events.impl.ProgressBuildEventImpl
import com.intellij.build.events.impl.StartBuildEventImpl
import com.intellij.build.events.impl.SuccessResultImpl
import io.kotest.matchers.maps.shouldContainExactly
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

private data class TestableBuildEvent(
  val eventType: KClass<*>,
  val id: Any?,
  val parentId: Any?,
  val message: String,
)

private class BuildProgressListenerTestMock : BuildProgressListener {

  val events: MutableMap<Any, List<TestableBuildEvent>> = mutableMapOf()

  override fun onEvent(buildId: Any, event: BuildEvent) {
    addEvent(buildId, sanitizeEvent(event))
  }

  private fun sanitizeEvent(eventToSanitize: BuildEvent): TestableBuildEvent = when (eventToSanitize) {
    is OutputBuildEventImpl -> TestableBuildEvent(
      eventToSanitize::class,
      null,
      eventToSanitize.parentId,
      eventToSanitize.message
    )

    else -> TestableBuildEvent(
      eventToSanitize::class,
      eventToSanitize.id,
      eventToSanitize.parentId,
      eventToSanitize.message
    )
  }

  private fun addEvent(buildId: Any, event: TestableBuildEvent) {
    events.merge(buildId, listOf(event)) { acc, x -> acc + x }
  }
}

class BspSyncConsoleTest {

  @Test
  fun `should start the import, start 3 subtask, put 2 messages and for each subtask and finish the import (the happy path)`() {
    // given
    val buildProcessListener = BuildProgressListenerTestMock()
    val basePath = "/project/"
    // when
    val bspSyncConsole = BspSyncConsole(buildProcessListener, basePath)

    bspSyncConsole.addMessage("task before start", "message before start - should be omitted")

    bspSyncConsole.startImport("import", "Import", "Importing...")

    bspSyncConsole.startSubtask("subtask 1", "Starting subtask 1")
    bspSyncConsole.addMessage("subtask 1", "message 1\n")
    bspSyncConsole.addMessage("subtask 1", "message 2") // should add new line at the end
    bspSyncConsole.finishSubtask("subtask 1", "Subtask 1 finished")

    bspSyncConsole.startSubtask("subtask 2", "Starting subtask 2")
    bspSyncConsole.addMessage("subtask 2", "message 3\n")
    bspSyncConsole.addMessage("subtask 2", "") // should be omitted - empty string

    bspSyncConsole.startSubtask("subtask 3", "Starting subtask 3")
    bspSyncConsole.addMessage("subtask 3", "message 4")
    bspSyncConsole.addMessage("subtask 3", "      ") // should be omitted - blank string

    bspSyncConsole.addMessage(null, "message 5")

    bspSyncConsole.addMessage("subtask 2", "message 6\n")
    bspSyncConsole.addMessage("subtask 3", "message 7\n")

    bspSyncConsole.finishSubtask("subtask 2", "Subtask 2 finished")
    bspSyncConsole.finishSubtask("subtask 3", "Subtask 3 finished")

    bspSyncConsole.finishImport("finitio!", SuccessResultImpl())

    bspSyncConsole.addMessage("task after finish", "message after finish - should be omitted")

    // then
    buildProcessListener.events shouldContainExactly mapOf(
      "import" to listOf(
        TestableBuildEvent(StartBuildEventImpl::class, "import", null, "Importing..."),

        TestableBuildEvent(ProgressBuildEventImpl::class, "subtask 1", "import", "Starting subtask 1"),
        TestableBuildEvent(OutputBuildEventImpl::class, null, "subtask 1", "message 1\n"),
        TestableBuildEvent(OutputBuildEventImpl::class, null, "subtask 1", "message 2\n"),
        TestableBuildEvent(FinishBuildEventImpl::class, "subtask 1", null, "Subtask 1 finished"),

        TestableBuildEvent(ProgressBuildEventImpl::class, "subtask 2", "import", "Starting subtask 2"),
        TestableBuildEvent(OutputBuildEventImpl::class, null, "subtask 2", "message 3\n"),

        TestableBuildEvent(ProgressBuildEventImpl::class, "subtask 3", "import", "Starting subtask 3"),
        TestableBuildEvent(OutputBuildEventImpl::class, null, "subtask 3", "message 4\n"),

        TestableBuildEvent(OutputBuildEventImpl::class, null, null, "message 5\n"),

        TestableBuildEvent(OutputBuildEventImpl::class, null, "subtask 2", "message 6\n"),
        TestableBuildEvent(OutputBuildEventImpl::class, null, "subtask 3", "message 7\n"),

        TestableBuildEvent(FinishBuildEventImpl::class, "subtask 2", null, "Subtask 2 finished"),
        TestableBuildEvent(FinishBuildEventImpl::class, "subtask 3", null, "Subtask 3 finished"),

        TestableBuildEvent(FinishBuildEventImpl::class, "import", null, "finitio!"),
      )
    )
  }
}
