package org.jetbrains.bazel.debug.connector

import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos as SDP
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.DebugEvent.PayloadCase
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import org.jetbrains.bazel.debug.platform.StarlarkBreakpointProperties
import org.jetbrains.bazel.debug.platform.StarlarkDebuggerEvaluator
import org.jetbrains.bazel.debug.platform.StarlarkSuspendContext
import java.util.concurrent.CompletableFuture

class ThreadAwareEventHandler(
  private val session: XDebugSession,
  private val messenger: StarlarkDebugMessenger,
  private val breakpointResolver: (String, Int) -> XLineBreakpoint<StarlarkBreakpointProperties>?,
) {
  // maps thread IDs to PausedThreads
  private val pausedThreads = mutableMapOf<Long, SDP.PausedThread>()
  private val valueComputer: StarlarkValueComputer = getValueComputer()
  private val evaluatorProvider = StarlarkDebuggerEvaluator.Provider(valueComputer, messenger)

  fun reactTo(event: SDP.DebugEvent) {
    when (event.payloadCase) {
      PayloadCase.ERROR -> handleError(event.error)
      PayloadCase.THREAD_PAUSED -> handleThreadPaused(event.threadPaused)
      PayloadCase.THREAD_CONTINUED -> handleThreadContinued(event.threadContinued)
      else -> {}
    }
  }

  /** Creates an immutable snapshot of paused thread set */
  private fun getPausedThreadList(): List<SDP.PausedThread> =
    pausedThreads.values.toList()

  private fun handleError(event: SDP.Error) {
    session.reportError(event.message)
  }

  private fun handleThreadPaused(event: SDP.ThreadPausedEvent) {
    val primaryThreadId = event.thread.id
    pausedThreads[primaryThreadId] = event.thread

    val suspendContext = StarlarkSuspendContext(
      threads = getPausedThreadList(),
      valueComputer = valueComputer,
      evaluatorProvider = evaluatorProvider,
      primaryThreadId = primaryThreadId,
    )

    when (event.thread.pauseReason) {
      SDP.PauseReason.HIT_BREAKPOINT -> handleBreakpointHit(event, suspendContext, primaryThreadId)
      SDP.PauseReason.INITIALIZING -> { /* ignore */ }
      else -> session.positionReached(suspendContext)
    }
  }

  private fun handleBreakpointHit(
    event: SDP.ThreadPausedEvent,
    suspendContext: StarlarkSuspendContext,
    primaryThreadId: Long,
  ) {
    val breakpoint =
      breakpointResolver(event.thread.location.path, event.thread.location.lineNumber)
    val shouldStop = breakpoint?.let {
      session.breakpointReached(it, null, suspendContext)
    }
    if (shouldStop != true) {
      messenger.resumeThread(primaryThreadId)
    }
  }

  private fun handleThreadContinued(event: SDP.ThreadContinuedEvent) {
    val threadId = event.threadId
    pausedThreads.remove(threadId)
  }

  private fun getValueComputer() = object : StarlarkValueComputer {
    override fun computeFramesForExecutionStack(threadId: Long): CompletableFuture<List<SDP.Frame>> =
      messenger.listFrames(threadId).thenApply { it?.frameList ?: emptyList() }

    override fun computeValueChildren(
      threadId: Long,
      valueId: Long,
    ): CompletableFuture<List<SDP.Value>> =
      messenger.getChildren(threadId, valueId).thenApply { it?.childrenList ?: emptyList() }
  }
}
