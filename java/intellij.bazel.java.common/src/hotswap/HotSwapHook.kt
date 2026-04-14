package org.jetbrains.bazel.hotswap

import com.intellij.debugger.impl.DebuggerSession
import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.annotations.ApiStatus

interface HotSwapHook {
  /**
   * Legacy method for backward compatibility.
   * Called when hotswap completes.
   */
  suspend fun onHotSwap(sessions: List<DebuggerSession>) {}

  /**
   * Called when hotswap process starts.
   */
  suspend fun onHotSwapStarted() {}

  /**
   * Called when modified files are detected and ready for hotswap.
   * @param fileInfo information about modified jar files and their classes
   */
  suspend fun onHotSwapFilesDetected(fileInfo: List<BazelHotSwapFileInfo>) {}

  /**
   * Called when hotswap process finishes with status information.
   * @param status the result status of the hotswap operation
   */
  suspend fun onHotSwapFinished(status: BazelHotSwapStatus) {}

  @ApiStatus.Internal
  companion object {
    internal val EP_NAME: ExtensionPointName<HotSwapHook> = ExtensionPointName.create("org.jetbrains.bazel.hotSwapHook")
  }
}
