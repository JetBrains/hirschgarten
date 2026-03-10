package org.jetbrains.bazel.server.sync

import com.jediterm.core.util.TermSize
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.TaskId

@ApiStatus.Internal
interface PtyTerminalService {
  fun ptyTermSize(taskId: TaskId): TermSize?
}
