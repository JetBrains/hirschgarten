package org.jetbrains.bazel.debug.connector

import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos as SDP
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import org.jetbrains.bazel.debug.platform.StarlarkBreakpointProperties
import java.io.Closeable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong

@Suppress("TooManyFunctions") // it serves as the main debug message dispatcher, it needs these methods
class StarlarkDebugMessenger(
  private val connector: StarlarkSocketConnector,
  private val session: XDebugSession,
) : Closeable {
  private val subscriptions = mutableMapOf<Long, CompletableFuture<SDP.DebugEvent>>()
  private val seq = AtomicLong(1L)

  /** This function needs to be safe to call multiple times */
  override fun close() {
    connector.close()
    session.stop()
  }

  fun readEventAndHandle(handler: ThreadAwareEventHandler) {
    val event = connector.read()
    val waitingForResponse = subscriptions[event.sequenceNumber]
    waitingForResponse?.complete(event) ?: handler.reactTo(event)
  }

  private fun debugRequestTemplate(): SDP.DebugRequest.Builder =
    SDP.DebugRequest.newBuilder().setSequenceNumber(seq.getAndIncrement())

  fun setBreakpoints(breakpoints: List<XLineBreakpoint<StarlarkBreakpointProperties>>) {
    val request = breakpoints
      .map { createSDPBreakpoint(it) }
      .let {
        SDP.SetBreakpointsRequest.newBuilder().addAllBreakpoint(it)
      }
    debugRequestTemplate().setSetBreakpoints(request).send()
  }

  fun listFrames(threadId: Long): CompletableFuture<SDP.ListFramesResponse?> {
    val request = SDP.ListFramesRequest.newBuilder()
      .setThreadId(threadId)
    return debugRequestTemplate().setListFrames(request).sendAndSubscribe { it.listFrames }
  }

  fun getChildren(threadId: Long, valueId: Long): CompletableFuture<SDP.GetChildrenResponse?> {
    val request = SDP.GetChildrenRequest.newBuilder()
      .setThreadId(threadId)
      .setValueId(valueId)
    return debugRequestTemplate().setGetChildren(request).sendAndSubscribe { it.getChildren }
  }

  fun evaluate(threadId: Long, expression: String): CompletableFuture<SDP.EvaluateResponse?> {
    val request = SDP.EvaluateRequest.newBuilder()
      .setThreadId(threadId)
      .setStatement(expression)
    return debugRequestTemplate().setEvaluate(request).sendAndSubscribe { it.evaluate }
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

  fun pauseAllThreads() {
    val request = SDP.PauseThreadRequest.newBuilder()
      .setThreadId(0)
    debugRequestTemplate().setPauseThread(request).send()
  }

  fun resumeAllThreads() {
    resumeThread(0)
  }

  fun resumeThread(threadId: Long, stepping: SDP.Stepping = SDP.Stepping.NONE) {
    val request = SDP.ContinueExecutionRequest.newBuilder()
      .setThreadId(threadId)
      .setStepping(stepping)
    debugRequestTemplate().setContinueExecution(request).send()
  }

  fun startDebugging() {
    val request = SDP.StartDebuggingRequest.newBuilder()
    debugRequestTemplate().setStartDebugging(request).send()
  }

  private fun SDP.DebugRequest.Builder.send() {
    try {
      connector.write(this.build())
    } catch (_: Exception) {
      close()
    }
  }

  private fun <ResponseT> SDP.DebugRequest.Builder.sendAndSubscribe(
    responseExtractor: (SDP.DebugEvent) -> ResponseT?,
  ): CompletableFuture<ResponseT?> {
    val responseFuture = ResponseFuture(responseExtractor)
    subscriptions[this.sequenceNumber] = responseFuture
    this.send()
    return responseFuture.typedResponseFuture
  }

  private class ResponseFuture<ResponseT>(
    private val responseExtractor: (SDP.DebugEvent) -> ResponseT?,
  ): CompletableFuture<SDP.DebugEvent>() {
    val typedResponseFuture: CompletableFuture<ResponseT?> = this.thenApply { responseExtractor(it) }
  }
}
