package org.jetbrains.bazel.debug

import com.intellij.execution.ui.ExecutionConsole
import com.intellij.openapi.util.Disposer
import io.kotest.matchers.booleans.shouldBeFalse
import kotlinx.coroutines.Job
import org.jetbrains.bazel.debug.connector.StarlarkDebugSessionManager
import org.jetbrains.bazel.debug.platform.StarlarkDebugProcess
import org.jetbrains.bazel.workspace.model.matchers.shouldBeEqual
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import javax.swing.JComponent
import javax.swing.JPanel
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos as SDP

class StarlarkDebugStopTest : StarlarkDebugClientTestBase() {
  @Test
  fun `stop button pressed should stop debugging`() {
    val conn = establishMockConnection()

    conn.sendFiveEventsToStream()
    conn.shouldStopDebugging {
      val process =
        StarlarkDebugProcess(
          connector = connector,
          session = session,
          breakpointHandler = breakpointHandler,
          console = NullExecutionConsole,
        )
      process.stop()
    }
  }

  @Test
  fun `socket EOF reached should stop debugging`() {
    val conn = establishMockConnection()
    conn.sendFiveEventsToStream()
    conn.shouldStopDebugging {
      socket.clearBuffers()
    }
  }

  // Unexpected exceptions are logged using Logger::error,
  //   which makes the following test fail.
  //   This will change once IJPL-453 issue is resolved.
  // TODO: Enable these tests once IJPL-453 is resolved
  @Disabled
  @Test
  fun `exception while reading should stop debugging`() {
    val conn = establishMockConnection()
    conn.sendFiveEventsToStream()
    conn.shouldStopDebugging {
      socket.simulateException(MockException())
    }
  }

  // Unexpected exceptions are logged using Logger::error,
  //   which makes the following test fail.
  //   This will change once IJPL-453 issue is resolved.
  // TODO: Enable these tests once IJPL-453 is resolved
  @Disabled
  @Test
  fun `exception while writing should stop debugging`() {
    val conn = establishMockConnection()
    conn.sendFiveEventsToStream()
    conn.shouldStopDebugging {
      socket.simulateException(MockException())
      messenger.startDebugging()
      socket.simulateException(null)
      // input stream will not throw an exception itself, but write exception encountered before should stop everything
    }
  }

  @Test
  fun `normal operation should not stop debugging`() {
    val conn = establishMockConnection()
    conn.sendFiveEventsToStream()
    conn.shouldNotStopDebugging {
      messenger.readEventAndHandle(conn.eventHandler)
      messenger.startDebugging()
    }
  }

  private fun MockConnectionPack.shouldStopDebugging(operation: MockConnectionPack.() -> Unit) {
    assertDebuggerStopped(true, operation)
  }

  private fun MockConnectionPack.shouldNotStopDebugging(operation: MockConnectionPack.() -> Unit) {
    assertDebuggerStopped(false, operation)
  }

  private fun MockConnectionPack.assertDebuggerStopped(shouldBeStopped: Boolean, operation: MockConnectionPack.() -> Unit) {
    withMockManager {
      val (job, managerLoopIteration) =
        getFutureAndLoopIteration(it)
      managerLoopIteration()

      // debugging should be running before the operation
      socket.isClosed.shouldBeFalse()
      job.isCompleted.shouldBeFalse()

      operation()
      managerLoopIteration()

      // after the operation debugging should have expected status
      socket.isClosed shouldBeEqual shouldBeStopped
      job.isCompleted shouldBeEqual shouldBeStopped
    }
  }

  private fun MockConnectionPack.getFutureAndLoopIteration(manager: StarlarkDebugSessionManager): Pair<Job, () -> Unit> {
    val managerLoopIteration =
      manager.getSingleLoopIteration(
        messenger = this.messenger,
        eventHandler = this.eventHandler,
      )
    val job = Job()
    manager.registerJobToCancel(job)

    return job to managerLoopIteration
  }

  private fun <T> withMockManager(action: (StarlarkDebugSessionManager) -> T): T {
    val manager = StarlarkDebugSessionManager(project)
    return try {
      action(manager)
    } finally {
      Disposer.dispose(manager)
    }
  }

  private fun MockConnectionPack.sendFiveEventsToStream() {
    val threadContinuedBuilder = SDP.ThreadContinuedEvent.newBuilder()
    repeat(5) {
      val threadId = 1000L + it
      val event =
        SDP.DebugEvent
          .newBuilder()
          .setThreadContinued(threadContinuedBuilder.setThreadId(threadId))
          .build()
      socket.sendMockEvent(event)
    }
  }
}

private object NullExecutionConsole : ExecutionConsole {
  override fun dispose() = Unit

  override fun getComponent(): JComponent = JPanel()

  override fun getPreferredFocusableComponent(): JComponent = JPanel()
}

private class MockException : Exception()
