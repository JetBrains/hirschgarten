package org.jetbrains.bazel.debug.connector

import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos as SDP

interface StarlarkValueComputer {
  /**
   * Computes the frames for the execution stack of a thread.
   *
   * @param threadId The ID of the thread.
   * @param callback The callback function that will be called with the computed frames.
   */
  fun computeFramesForExecutionStack(threadId: Long, callback: (List<SDP.Frame>) -> Unit)

  /**
   * Computes the children of a given compound value (i.e. list, dictionary etc.).
   *
   * @param threadId The ID of the thread.
   * @param valueId The ID of the value whose children values need to be computed.
   * @param callback The callback function that will be called with the computed children values.
   */
  fun computeValueChildren(
    threadId: Long,
    valueId: Long,
    callback: (List<SDP.Value>) -> Unit,
  )
}
