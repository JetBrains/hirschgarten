package org.jetbrains.bazel.ui.console

import com.intellij.openapi.project.Project
import com.jediterm.core.util.TermSize
import org.jetbrains.bazel.progress.ConsoleService
import org.jetbrains.bazel.progress.PtyAwareTaskConsole
import org.jetbrains.bazel.server.sync.PtyTerminalService
import org.jetbrains.bsp.protocol.TaskId

internal class DefaultPtyTerminalService(private val project: Project) : PtyTerminalService {
  override fun ptyTermSize(taskId: TaskId): TermSize? {
    val buildConsole = ConsoleService.getInstance(project).buildConsole
    val syncConsole = ConsoleService.getInstance(project).syncConsole
    if (buildConsole is PtyAwareTaskConsole) {
      buildConsole.ptyTermSize(taskId)?.let { return it }
    }
    if (syncConsole is PtyAwareTaskConsole) {
      syncConsole.ptyTermSize(taskId)?.let { return it }
    }
    return null
  }

}
