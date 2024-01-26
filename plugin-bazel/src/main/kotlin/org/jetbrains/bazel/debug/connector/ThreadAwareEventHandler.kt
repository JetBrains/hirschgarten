package org.jetbrains.bazel.debug.connector

import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos as SDP
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.DebugEvent.PayloadCase
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import org.jetbrains.bazel.debug.error.StarlarkDebuggerError
import org.jetbrains.bazel.debug.platform.StarlarkBreakpointProperties
import org.jetbrains.bazel.debug.platform.StarlarkDebuggerEvaluator
import org.jetbrains.bazel.debug.platform.StarlarkSuspendContext
import java.util.Collections

class ThreadAwareEventHandler(
  private val session: XDebugSession,
  private val messenger: StarlarkDebugMessenger,
  private val breakpointResolver: (String, Int) -> XLineBreakpoint<StarlarkBreakpointProperties>?
) {
  // maps thread IDs to PausedThreads
  private val pausedThreads = Collections.synchronizedMap(mutableMapOf<Long, SDP.PausedThread>())
  private val subscriptions = Collections.synchronizedMap(mutableMapOf<Long, (SDP.DebugEvent) -> Unit>())

  private val evaluatorProvider = StarlarkDebuggerEvaluator.Provider(this)

  val valueComputer: StarlarkValueComputer = this.ValueComputer()

  fun reactTo(event: SDP.DebugEvent) {
    subscriptions.remove(event.sequenceNumber)?.let {
      it(event)
    } ?: when (event.payloadCase) {
      PayloadCase.ERROR -> handleError(event.error)
      PayloadCase.THREAD_PAUSED -> handleThreadPaused(event.threadPaused)
      PayloadCase.THREAD_CONTINUED -> handleThreadContinued(event.threadContinued)
      else -> {}
    }
  }

  /** Creates an immutable snapshot of paused thread set */
  private fun getPausedThreadList(): List<SDP.PausedThread> = synchronized(pausedThreads) { pausedThreads.values.toList() }

  private fun handleError(event: SDP.Error) {
    throw StarlarkDebuggerError(event)
  }

  private fun handleThreadPaused(event: SDP.ThreadPausedEvent) {
    val primaryThreadId = event.thread.id
    pausedThreads[primaryThreadId] = event.thread

    val suspendContext = StarlarkSuspendContext(
      threads = getPausedThreadList(),
      valueComputer = valueComputer,
      evaluatorProvider = evaluatorProvider,
      primaryThreadId = primaryThreadId
    )

    if (event.thread.pauseReason == SDP.PauseReason.HIT_BREAKPOINT) {
      val breakpoint =
        breakpointResolver(event.thread.location.path, event.thread.location.lineNumber)
      val shouldStop = breakpoint?.let {
        session.breakpointReached(it, null, suspendContext)
      }
      if (shouldStop != true) {
        messenger.resumeThread(primaryThreadId)
      }
    } else {
      session.positionReached(suspendContext)
    }
  }

  private fun handleThreadContinued(event: SDP.ThreadContinuedEvent) {
    val threadId = event.threadId
    pausedThreads.remove(threadId)
  }

  private fun StarlarkDebugMessenger.RequestReadyToSend.subscribeAndSend(callback: (SDP.DebugEvent) -> Unit) {
    subscriptions[this.getSequenceNumber()] = callback
    this.send()
  }

  fun evaluate(threadId: Long, expression: String, consumer: (SDP.Value?) -> Unit) {
    messenger.prepareEvaluate(threadId, expression).subscribeAndSend {
      consumer(it.evaluate?.result)
    }
  }


  private inner class ValueComputer : StarlarkValueComputer {
    override fun computeFramesForExecutionStack(
      threadId: Long,
      callback: (List<SDP.Frame>) -> Unit,
    ) {
      messenger.prepareListFrames(threadId).subscribeAndSend {
        callback(it.listFrames?.frameList ?: emptyList())
      }
    }

    override fun computeValueChildren(
      threadId: Long,
      valueId: Long,
      callback: (List<SDP.Value>) -> Unit,
    ) {
      messenger.prepareGetChildren(threadId, valueId).subscribeAndSend {
        callback(it.getChildren?.childrenList ?: emptyList())
      }
    }
  }
}
