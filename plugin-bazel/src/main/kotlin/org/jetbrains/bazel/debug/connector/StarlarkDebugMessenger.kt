package org.jetbrains.bazel.debug.connector

import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos as SDP
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import org.jetbrains.bazel.debug.platform.StarlarkBreakpointProperties
import java.util.concurrent.atomic.AtomicLong

class StarlarkDebugMessenger(
  private val connector: StarlarkSocketConnector,
  private val session: XDebugSession,
) {
  private val seq = AtomicLong(1L)

  /** This function needs to be safe to call multiple times */
  fun close() {
    connector.close()
    session.stop()
  }

  fun isClosed(): Boolean = connector.isClosed()

  fun readEventAndHandle(handler: ThreadAwareEventHandler) {
    val event = connector.read()
    event?.let { handler.reactTo(it) }
  }

  private fun debugRequestTemplate(): SDP.DebugRequest.Builder =
    SDP.DebugRequest.newBuilder().setSequenceNumber(seq.getAndIncrement())

  /** @return sequence number of the request that was sent */
  private fun SDP.DebugRequest.Builder.send(): Long = this.build() sendThrough connector

  private fun SDP.DebugRequest.Builder.prepare() = RequestReadyToSend(this.build())

  fun setBreakpoints(breakpoints: List<XLineBreakpoint<StarlarkBreakpointProperties>>): Long {
    val request = breakpoints
      .map { createSDPBreakpoint(it) }
      .let {
        SDP.SetBreakpointsRequest.newBuilder().addAllBreakpoint(it)
      }
    return debugRequestTemplate().setSetBreakpoints(request).send()
  }

  fun prepareListFrames(threadId: Long): RequestReadyToSend {
    val request = SDP.ListFramesRequest.newBuilder()
      .setThreadId(threadId)
    return debugRequestTemplate().setListFrames(request).prepare()
  }

  fun prepareGetChildren(threadId: Long, valueId: Long): RequestReadyToSend {
    val request = SDP.GetChildrenRequest.newBuilder()
      .setThreadId(threadId)
      .setValueId(valueId)
    return debugRequestTemplate().setGetChildren(request).prepare()
  }

  fun prepareEvaluate(threadId: Long, expression: String): RequestReadyToSend {
    val request = SDP.EvaluateRequest.newBuilder()
      .setThreadId(threadId)
      .setStatement(expression)
    return debugRequestTemplate().setEvaluate(request).prepare()
  }

  private fun createSDPBreakpoint(breakpoint: XLineBreakpoint<StarlarkBreakpointProperties>): SDP.Breakpoint {
    val path = breakpoint.presentableFilePath
    val line = breakpoint.line + 1  // XLineBreakpoints' lines are 0-indexed; we want 1-indexed
    val location = SDP.Location.newBuilder()
      .setPath(path)
      .setLineNumber(line)
    return SDP.Breakpoint.newBuilder()
      .setLocation(location)
      .build()
  }

  fun pauseAllThreads(): Long {
    val request = SDP.PauseThreadRequest.newBuilder()
      .setThreadId(0)
    return debugRequestTemplate().setPauseThread(request).send()
  }

  fun resumeAllThreads(): Long = resumeThread(0)

  fun resumeThread(threadId: Long, stepping: SDP.Stepping = SDP.Stepping.NONE): Long {
    val request = SDP.ContinueExecutionRequest.newBuilder()
      .setThreadId(threadId)
      .setStepping(stepping)
    return debugRequestTemplate().setContinueExecution(request).send()
  }

  fun startDebugging(): Long {
    val request = SDP.StartDebuggingRequest.newBuilder()
    return debugRequestTemplate().setStartDebugging(request).send()
  }

  /** Allows other classes to send requests without showing implementation details */
  inner class RequestReadyToSend(private val request: SDP.DebugRequest) {
    fun send(): Long = request sendThrough connector
    fun getSequenceNumber(): Long = request.sequenceNumber
  }
}

private infix fun SDP.DebugRequest.sendThrough(connector: StarlarkSocketConnector): Long {
  connector.write(this)
  return this.sequenceNumber
}
