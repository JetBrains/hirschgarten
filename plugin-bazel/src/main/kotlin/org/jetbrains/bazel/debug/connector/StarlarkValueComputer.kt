package org.jetbrains.bazel.debug.connector

import java.util.concurrent.CompletableFuture
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos as SDP

interface StarlarkValueComputer {
  /**
   * Computes the frames for the execution stack of a thread.
   *
   * @param threadId The ID of the thread.
   */
  fun computeFramesForExecutionStack(threadId: Long): CompletableFuture<List<SDP.Frame>>

  /**
   * Computes the children of a given compound value (i.e. list, dictionary etc.).
   *
   * @param threadId The ID of the thread.
   * @param valueId The ID of the value whose children values need to be computed.
   */
  fun computeValueChildren(
    threadId: Long,
    valueId: Long,
  ): CompletableFuture<List<SDP.Value>>
}
