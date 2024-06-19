package org.jetbrains.bazel.debug

import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos as SDP
import com.intellij.execution.ui.ExecutionConsole
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual
import org.jetbrains.bazel.debug.connector.StarlarkDebugManager
import org.jetbrains.bazel.debug.platform.StarlarkDebugProcess
import org.jetbrains.bazel.debug.utils.TestProjectGenerator
import org.jetbrains.bsp.protocol.AnalysisDebugResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import javax.swing.JComponent
import javax.swing.JPanel

class StarlarkDebugStopTest : StarlarkDebugClientTestBase() {
  private lateinit var managerLoopIteration: () -> Unit
  private lateinit var future: CompletableFuture<AnalysisDebugResult>

  @BeforeEach
  override fun establishMockConnection() {
    super.establishMockConnection()

    val project = TestProjectGenerator.createProject()

    val manager = StarlarkDebugManager(project)
    managerLoopIteration = manager.startDryAndGetLoopIteration(
      messenger = messenger,
      breakpointHandler = breakpointHandler,
      eventHandler = eventHandler,
    )

    future = CompletableFuture()
    manager.registerFutureToStop(future)
  }

  @Test
  fun `stop button pressed should stop debugging`() {
//  fun testStopButton() {
    sendFiveEventsToStream()
    socket.isClosed.shouldBeFalse()
    future.isDone.shouldBeFalse()

    val process = StarlarkDebugProcess(
      connector = connector,
      session = session,
      breakpointHandler = breakpointHandler,
      console = NullExecutionConsole,
    )
    process.stop()
    managerLoopIteration()

    socket.isClosed.shouldBeTrue()
    future.isDone.shouldBeTrue()
  }

  @Test
  fun `socket EOF reached should stop debugging`() {
    sendFiveEventsToStream()
    shouldStopDebugging {
      socket.clearBuffers()
    }
  }

  @Test
  fun `exception while reading should stop debugging`() {
    sendFiveEventsToStream()
    shouldStopDebugging {
      socket.simulateException(MockException())
    }
  }

  @Test
  fun `exception while writing should stop debugging`() {
    sendFiveEventsToStream()
    shouldStopDebugging {
      socket.simulateException(MockException())
      messenger.startDebugging()
      socket.simulateException(null)
      // input stream will not throw an exception itself, but write exception encountered before should stop everything
    }
  }

  @Test
  fun `normal operation should not stop debugging`() {
    sendFiveEventsToStream()
    shouldNotStopDebugging {
      messenger.readEventAndHandle(eventHandler)
      messenger.startDebugging()
    }
  }

  private fun shouldStopDebugging(operation: () -> Unit) {
    assertDebuggerStopped(true, operation)
  }

  private fun shouldNotStopDebugging(operation: () -> Unit) {
    assertDebuggerStopped(false, operation)
  }

  private fun assertDebuggerStopped(shouldBeStopped: Boolean, operation: () -> Unit) {
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

  private fun sendFiveEventsToStream() {
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
