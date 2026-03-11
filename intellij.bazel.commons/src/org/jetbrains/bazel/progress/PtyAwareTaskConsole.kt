package org.jetbrains.bazel.progress

import com.jediterm.core.util.TermSize
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.TaskId

@ApiStatus.Internal
interface PtyAwareTaskConsole {
  fun ptyTermSize(taskId: TaskId): TermSize?
}
