package org.jetbrains.bazel.debug

import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos as SDP
import com.intellij.execution.ui.ExecutionConsole
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.equals.shouldBeEqual
import org.jetbrains.bazel.debug.connector.StarlarkDebugSessionManager
import org.jetbrains.bazel.debug.platform.StarlarkDebugProcess
import org.jetbrains.bazel.debug.utils.TestProjectGenerator
import org.jetbrains.bsp.protocol.AnalysisDebugResult
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import javax.swing.JComponent
import javax.swing.JPanel

class StarlarkDebugStopTest : StarlarkDebugClientTestBase() {
  @Test
  fun `stop button pressed should stop debugging`() {
    val conn = establishMockConnection()

    conn.sendFiveEventsToStream()
    conn.shouldStopDebugging {
      val process = StarlarkDebugProcess(
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

  @Test
  fun `exception while reading should stop debugging`() {
    val conn = establishMockConnection()
    conn.sendFiveEventsToStream()
    conn.shouldStopDebugging {
      socket.simulateException(MockException())
    }
  }

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

  private fun MockConnectionPack.assertDebuggerStopped(
    shouldBeStopped: Boolean,
    operation: MockConnectionPack.() -> Unit,
  ) {
    val (future, managerLoopIteration) =
      getFutureAndLoopIteration()
    managerLoopIteration()

    // debugging should be running before the operation
    socket.isClosed.shouldBeFalse()
    future.isDone.shouldBeFalse()

    operation()
    managerLoopIteration()

    // after the operation debugging should have expected status
    socket.isClosed shouldBeEqual shouldBeStopped
    future.isDone shouldBeEqual shouldBeStopped
  }

  private fun MockConnectionPack.getFutureAndLoopIteration(): Pair<CompletableFuture<AnalysisDebugResult>, () -> Unit> {
    val project = TestProjectGenerator.createProject()
    val manager = StarlarkDebugSessionManager(project)
    val managerLoopIteration = manager.startDryAndGetLoopIteration(
      messenger = messenger,
      eventHandler = eventHandler,
    )
    val future = CompletableFuture<AnalysisDebugResult>()
    manager.registerFutureToStop(future)

    return future to managerLoopIteration
  }

  private fun MockConnectionPack.sendFiveEventsToStream() {
    val threadContinuedBuilder = SDP.ThreadContinuedEvent.newBuilder()
    repeat(5) {
      val threadId = 1000L + it
      val event =
        SDP.DebugEvent.newBuilder().setThreadContinued(threadContinuedBuilder.setThreadId(threadId)).build()
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
