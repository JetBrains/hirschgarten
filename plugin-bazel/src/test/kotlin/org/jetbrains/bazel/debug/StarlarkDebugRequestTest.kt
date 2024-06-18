package org.jetbrains.bazel.debug

import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos as SDP
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import org.junit.jupiter.api.Test

class StarlarkDebugRequestTest : StarlarkDebugClientTestBase() {
  @Test
  fun `start debugging request`() {
    val seq = messenger.startDebugging()
    val singleRequest = socket.readRequests().singleOrNull()

    singleRequest.shouldNotBeNull()
    seq shouldBeEqual singleRequest.sequenceNumber
    singleRequest.payloadCase shouldBeEqual SDP.DebugRequest.PayloadCase.START_DEBUGGING
  }

  @Test
  fun `list frames request`() {
    val seq = messenger.prepareListFrames(THREAD_ID).send()
    val singleRequest = socket.readRequests().singleOrNull()

    singleRequest.shouldNotBeNull()
    seq shouldBeEqual singleRequest.sequenceNumber
    singleRequest.payloadCase shouldBeEqual SDP.DebugRequest.PayloadCase.LIST_FRAMES
    singleRequest.listFrames.threadId shouldBeEqual THREAD_ID
  }

  @Test
  fun `get children request`() {
    val seq = messenger.prepareGetChildren(THREAD_ID, VALUE_ID).send()
    val singleRequest = socket.readRequests().singleOrNull()

    singleRequest.shouldNotBeNull()
    seq shouldBeEqual singleRequest.sequenceNumber
    singleRequest.payloadCase shouldBeEqual SDP.DebugRequest.PayloadCase.GET_CHILDREN
    singleRequest.getChildren.threadId shouldBeEqual THREAD_ID
    singleRequest.getChildren.valueId shouldBeEqual VALUE_ID
  }

  @Test
  fun `evaluate request`() {
    val seq = messenger.prepareEvaluate(THREAD_ID, EXPRESSION).send()
    val singleRequest = socket.readRequests().singleOrNull()

    singleRequest.shouldNotBeNull()
    seq shouldBeEqual singleRequest.sequenceNumber
    singleRequest.payloadCase shouldBeEqual SDP.DebugRequest.PayloadCase.EVALUATE
    singleRequest.evaluate.threadId shouldBeEqual THREAD_ID
    singleRequest.evaluate.statement shouldBeEqual EXPRESSION
  }

  @Test
  fun `pause all threads request`() {
    val seq = messenger.pauseAllThreads()
    val singleRequest = socket.readRequests().singleOrNull()

    singleRequest.shouldNotBeNull()
    seq shouldBeEqual singleRequest.sequenceNumber
    singleRequest.payloadCase shouldBeEqual SDP.DebugRequest.PayloadCase.PAUSE_THREAD
    singleRequest.pauseThread.threadId shouldBeEqual 0
  }

  @Test
  fun `resume all threads request`() {
    val seq = messenger.resumeAllThreads()
    val singleRequest = socket.readRequests().singleOrNull()

    singleRequest.shouldNotBeNull()
    seq shouldBeEqual singleRequest.sequenceNumber
    singleRequest.payloadCase shouldBeEqual SDP.DebugRequest.PayloadCase.CONTINUE_EXECUTION
    singleRequest.continueExecution.threadId shouldBeEqual 0
    singleRequest.continueExecution.stepping shouldBeEqual SDP.Stepping.NONE
  }

  @Test
  fun `resume a single thread request`() {
    val seq = messenger.resumeThread(THREAD_ID)
    val singleRequest = socket.readRequests().singleOrNull()

    singleRequest.shouldNotBeNull()
    seq shouldBeEqual singleRequest.sequenceNumber
    singleRequest.payloadCase shouldBeEqual SDP.DebugRequest.PayloadCase.CONTINUE_EXECUTION
    singleRequest.continueExecution.threadId shouldBeEqual THREAD_ID
    singleRequest.continueExecution.stepping shouldBeEqual SDP.Stepping.NONE
  }

  @Test
  fun `step over request`() {
    val seq = messenger.resumeThread(THREAD_ID, SDP.Stepping.OVER)
    val singleRequest = socket.readRequests().singleOrNull()

    singleRequest.shouldNotBeNull()
    seq shouldBeEqual singleRequest.sequenceNumber
    singleRequest.payloadCase shouldBeEqual SDP.DebugRequest.PayloadCase.CONTINUE_EXECUTION
    singleRequest.continueExecution.threadId shouldBeEqual THREAD_ID
    singleRequest.continueExecution.stepping shouldBeEqual SDP.Stepping.OVER
  }
}

private const val THREAD_ID = 12345L
private const val VALUE_ID = 67890L
private const val EXPRESSION = "2 + 2 * 2"
