package org.jetbrains.bazel.debug

import io.kotest.matchers.nulls.shouldNotBeNull
import org.jetbrains.bazel.debug.connector.StarlarkDebugMessenger
import org.jetbrains.bazel.debug.utils.MockSocket
import org.jetbrains.bazel.workspace.model.matchers.shouldBeEqual
import org.junit.jupiter.api.Test
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos as SDP

class StarlarkDebugRequestTest : StarlarkDebugClientTestBase() {
  @Test
  fun `start debugging request`() {
    val (socket, messenger) = getSocketAndMessenger()
    messenger.startDebugging()
    val singleRequest = socket.readRequests().singleOrNull()

    singleRequest.shouldNotBeNull()
    singleRequest.payloadCase shouldBeEqual SDP.DebugRequest.PayloadCase.START_DEBUGGING
  }

  @Test
  fun `list frames request`() {
    val (socket, messenger) = getSocketAndMessenger()
    messenger.listFrames(THREAD_ID)
    val singleRequest = socket.readRequests().singleOrNull()

    singleRequest.shouldNotBeNull()
    singleRequest.payloadCase shouldBeEqual SDP.DebugRequest.PayloadCase.LIST_FRAMES
    singleRequest.listFrames.threadId shouldBeEqual THREAD_ID
  }

  @Test
  fun `get children request`() {
    val (socket, messenger) = getSocketAndMessenger()
    messenger.getChildren(THREAD_ID, VALUE_ID)
    val singleRequest = socket.readRequests().singleOrNull()

    singleRequest.shouldNotBeNull()
    singleRequest.payloadCase shouldBeEqual SDP.DebugRequest.PayloadCase.GET_CHILDREN
    singleRequest.getChildren.threadId shouldBeEqual THREAD_ID
    singleRequest.getChildren.valueId shouldBeEqual VALUE_ID
  }

  @Test
  fun `evaluate request`() {
    val (socket, messenger) = getSocketAndMessenger()
    messenger.evaluate(THREAD_ID, EXPRESSION)
    val singleRequest = socket.readRequests().singleOrNull()

    singleRequest.shouldNotBeNull()
    singleRequest.payloadCase shouldBeEqual SDP.DebugRequest.PayloadCase.EVALUATE
    singleRequest.evaluate.threadId shouldBeEqual THREAD_ID
    singleRequest.evaluate.statement shouldBeEqual EXPRESSION
  }

  @Test
  fun `pause all threads request`() {
    val (socket, messenger) = getSocketAndMessenger()
    messenger.pauseAllThreads()
    val singleRequest = socket.readRequests().singleOrNull()

    singleRequest.shouldNotBeNull()
    singleRequest.payloadCase shouldBeEqual SDP.DebugRequest.PayloadCase.PAUSE_THREAD
    singleRequest.pauseThread.threadId shouldBeEqual 0
  }

  @Test
  fun `resume all threads request`() {
    val (socket, messenger) = getSocketAndMessenger()
    messenger.resumeAllThreads()
    val singleRequest = socket.readRequests().singleOrNull()

    singleRequest.shouldNotBeNull()
    singleRequest.payloadCase shouldBeEqual SDP.DebugRequest.PayloadCase.CONTINUE_EXECUTION
    singleRequest.continueExecution.threadId shouldBeEqual 0
    singleRequest.continueExecution.stepping shouldBeEqual SDP.Stepping.NONE
  }

  @Test
  fun `resume a single thread request`() {
    val (socket, messenger) = getSocketAndMessenger()
    messenger.resumeThread(THREAD_ID)
    val singleRequest = socket.readRequests().singleOrNull()

    singleRequest.shouldNotBeNull()
    singleRequest.payloadCase shouldBeEqual SDP.DebugRequest.PayloadCase.CONTINUE_EXECUTION
    singleRequest.continueExecution.threadId shouldBeEqual THREAD_ID
    singleRequest.continueExecution.stepping shouldBeEqual SDP.Stepping.NONE
  }

  @Test
  fun `step over request`() {
    val (socket, messenger) = getSocketAndMessenger()
    messenger.resumeThread(THREAD_ID, SDP.Stepping.OVER)
    val singleRequest = socket.readRequests().singleOrNull()

    singleRequest.shouldNotBeNull()
    singleRequest.payloadCase shouldBeEqual SDP.DebugRequest.PayloadCase.CONTINUE_EXECUTION
    singleRequest.continueExecution.threadId shouldBeEqual THREAD_ID
    singleRequest.continueExecution.stepping shouldBeEqual SDP.Stepping.OVER
  }

  private fun getSocketAndMessenger(): Pair<MockSocket, StarlarkDebugMessenger> =
    establishMockConnection().let { it.socket to it.messenger }
}

private const val THREAD_ID = 12345L
private const val VALUE_ID = 67890L
private const val EXPRESSION = "2 + 2 * 2"
